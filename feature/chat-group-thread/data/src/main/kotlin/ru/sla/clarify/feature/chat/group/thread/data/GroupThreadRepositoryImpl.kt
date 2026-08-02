package ru.sla.clarify.feature.chat.group.thread.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chat.group.thread.domain.GroupThreadRepository
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.domain.entity.FoundUser
import ru.sla.clarify.feature.chat.group.thread.domain.entity.Group
import ru.sla.clarify.feature.chat.group.thread.domain.entity.GroupMember
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.entity.MemberNM
import ru.sla.clarify.mapper.data.mapToCommit
import ru.sla.clarify.mapper.data.toDomainModel
import ru.sla.log.log
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.time.LocalDateTime

@SingleIn(GroupThreadScope::class)
@ContributesBinding(GroupThreadScope::class)
class GroupThreadRepositoryImpl @Inject constructor(
  target: TargetParams,
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) : GroupThreadRepository {

  private val conversationId = target.conversationId
  private val lastReadWatermark = MutableStateFlow<LocalDateTime?>(null)

  override suspend fun subscribeOnCommitChanges() {
    val userId = requireUserId()
    firestore.groupCommitsLive(
      conversationId = conversationId.value,
      limit = LIVE_COMMIT_LIMIT
    ).collect { changes ->
      applyCommitChanges(
        userId = userId,
        changes = changes
      )
    }
  }

  override suspend fun fetchHistoryCommits(count: Int) {
    val historyCommits = firestore.readCommits(
      conversationId = conversationId.value,
      branchId = conversationId.value,
      limit = count.toLong(),
      before = null
    )
    val userId = requireUserId()
    withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        historyCommits.forEach { commit ->
          applyInsertOrReplaceCommit(
            commit = commit,
            userId = userId,
            hasPendingWrites = false
          )
        }
      }
    }
  }

  override suspend fun sendCommit(text: String) {
    val memberUids = withContext(Dispatchers.IO) { currentMemberUids() }
    firestore.createGroupCommit(
      conversationId = conversationId.value,
      text = text,
      memberUids = memberUids
    )
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    val current = lastReadWatermark.value
    if (current != null && !lastReadAt.isAfter(current)) {
      log { "Group: lastReadAt ($lastReadAt) is not after current watermark ($current), skipping" }
      return
    }
    lastReadWatermark.value = lastReadAt
    firestore.updateReadWatermark(
      conversationId = conversationId.value,
      lastReadAt = lastReadAt
    )
    firestore.updateUnreadCount(
      conversationId = conversationId.value
    )
  }

  override suspend fun subscribeOnGroupMembers() {
    firestore.membersLive(conversationId.value)
      .collect(::applyMemberChanges)
  }

  override suspend fun renameGroup(name: String) {
    withContext(Dispatchers.IO) {
      firestore.updateConversationName(
        conversationId = conversationId.value,
        name = name
      )
      inMemoryDB.chatConversationQueries.updateName(
        id = conversationId,
        name = name
      )
    }
  }

  override suspend fun deleteConversation() {
    withContext(Dispatchers.IO) {
      firestore.deleteConversation(conversationId.value)
      inMemoryDB.transaction {
        inMemoryDB.chatConversationQueries.delete(conversationId)
        inMemoryDB.chatMemberQueries.delete(conversationId)
      }
    }
  }

  override suspend fun leaveConversation() {
    withContext(Dispatchers.IO) {
      firestore.deleteConversationMember(conversationId.value)
      inMemoryDB.transaction {
        inMemoryDB.chatConversationQueries.delete(conversationId)
        inMemoryDB.chatMemberQueries.delete(conversationId)
      }
    }
  }

  override suspend fun inviteGroupMembers(ids: List<Member.Id>) {
    withContext(Dispatchers.IO) {
      // visibleFor приглашающих системных коммитов — состав группы уже с учётом приглашённых,
      // чтобы новый участник видел «X пригласил Y» (запросы фильтруются по visibleFor).
      val visibleFor = (currentMemberUids() + ids.map { it.value }).distinct()
      ids.forEach { member ->
        firestore.createCommitInviteMember(
          conversationId = conversationId.value,
          memberId = member.value,
          memberUids = visibleFor
        )
      }
      inMemoryDB.transaction {
        ids.forEach { member ->
          inMemoryDB.chatMemberQueries.insertOrReplace(
            conversationId = conversationId,
            id = member
          )
        }
      }
    }
  }

  override suspend fun deleteConversationMember(id: Member.Id) {
    withContext(Dispatchers.IO) {
      firestore.deleteConversationMember(
        conversationId = conversationId.value,
        memberId = id.value
      )
      inMemoryDB.chatMemberQueries.deleteById(
        id = id,
        conversationId = conversationId
      )
    }
  }

  override suspend fun searchMemberByPrefix(prefix: String): List<FoundUser> {
    return firestore.readUsersByEmailPrefix(
      prefix = prefix.lowercase(),
      limit = USER_SEARCH_LIMIT
    ).map { user ->
      FoundUser(
        id = UserId(user.id),
        displayName = user.displayName,
        email = user.email,
        photoUrl = user.photoUrl
      )
    }
  }

  override fun observeGroup(): Flow<Group?> {
    return inMemoryDB.chatConversationQueries
      .selectGroup(
        id = conversationId,
        mapper = { rowId, name, ownerId, _, _, _, _, memberCount ->
          Group(
            id = rowId,
            name = name.orEmpty(),
            ownerId = ownerId ?: UserId(""),
            memberCount = memberCount.toInt()
          )
        }
      )
      .observeOneOrNull()
  }

  override fun observeGroupMembers(): Flow<List<GroupMember>> = flow {
    val currentUserId = authSessionPersistence.withKey { readUserId(it) }?.value
    val group = observeGroup()
    val members = inMemoryDB.chatMemberQueries
      .selectGroup(
        conversationId = conversationId,
        mapper = { memberId, displayName, email, photoUrl ->
          GroupMember(
            id = UserId(memberId.value),
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            isOwner = false,
            isMe = memberId.value == currentUserId
          )
        }
      )
      .observeList()
    combine(group, members) { groupValue, memberRows ->
      val ownerUid = groupValue?.ownerId?.value
      memberRows.map { member ->
        member.copy(isOwner = member.id.value == ownerUid)
      }
    }.collect { emit(it) }
  }

  private fun currentMemberUids(): List<String> {
    return inMemoryDB.chatMemberQueries
      .selectIds(conversationId)
      .executeAsList()
      .map { it.value }
  }

  override val commits: Flow<List<Commit>> = flow {
    inMemoryDB.chatCommitQueries
      .select(conversationId, Branch.Id(conversationId.value), ::mapToCommit)
      .observeList()
      .collect { emit(it) }
  }

  override val unreadCount: Flow<Long> = flow {
    firestore.unreadCountLive(conversationId.value)
      .collect { emit(it) }
  }

  private suspend fun applyCommitChanges(
    userId: UserId,
    changes: List<FirestoreChange<CommitNM>>
  ): Unit = withContext(Dispatchers.IO) {
    inMemoryDB.transaction {
      changes.forEach { change ->
        when (change.changeType) {
          FirestoreDocumentResult.Removed -> {
            inMemoryDB.chatCommitQueries.delete(Commit.Id(change.data.id))
          }
          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            applyInsertOrReplaceCommit(
              commit = change.data,
              userId = userId,
              hasPendingWrites = change.hasPendingWrites
            )
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceCommit(
    commit: CommitNM,
    userId: UserId,
    hasPendingWrites: Boolean
  ) {
    val row = commit.toDomainModel(
      conversationId = conversationId,
      selfUserId = userId,
      hasPendingWrites = hasPendingWrites
    )
    inMemoryDB.chatCommitQueries.insertOrReplace(
      id = row.id,
      conversationId = row.conversationId,
      branchId = row.branchId,
      senderId = row.senderId,
      type = row.type,
      text = row.text,
      invitedId = row.invitedId,
      createdAtNanos = row.createdAtNanos,
      isSelf = row.isSelf,
      status = row.status,
      editedAtNanos = row.editedAtNanos
    )
  }

  private suspend fun applyMemberChanges(changes: List<FirestoreChange<MemberNM>>) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        changes.forEach { change ->
          when (change.changeType) {
            FirestoreDocumentResult.Added,
            FirestoreDocumentResult.Modified -> {
              inMemoryDB.chatMemberQueries.insertOrReplace(
                conversationId = conversationId,
                id = Member.Id(change.data.id)
              )
            }
            FirestoreDocumentResult.Removed -> {
              inMemoryDB.chatMemberQueries.deleteById(
                id = Member.Id(change.data.id),
                conversationId = conversationId
              )
            }
          }
        }
      }
    }
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) })
  }
}

private const val LIVE_COMMIT_LIMIT = 30L
private const val USER_SEARCH_LIMIT = 10L
