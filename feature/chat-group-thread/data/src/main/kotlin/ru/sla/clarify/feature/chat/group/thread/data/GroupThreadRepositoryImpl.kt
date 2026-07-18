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
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.chat.ChatCommit
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation
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
import ru.sla.clarify.lib.google.firestore.toEpochNanos
import ru.sla.clarify.mapper.data.mapToCommit
import ru.sla.log.log
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.time.LocalDateTime

@SingleIn(GroupThreadScope::class)
@ContributesBinding(GroupThreadScope::class)
class GroupThreadRepositoryImpl @Inject constructor(
  target: TargetParams,
  private val firestore: Firestore,
  private val persistedDB: PersistedDB,
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

  override suspend fun fetchHistoryCommits(count: Int, before: Commit?) {
    val historyCommits = firestore.readCommits(
      conversationId = conversationId.value,
      branchId = conversationId.value,
      limit = count.toLong(),
      before = null
    )
    val userId = requireUserId()
    withContext(Dispatchers.IO) {
      persistedDB.transaction {
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
    val memberUids = withContext(Dispatchers.IO) {
      persistedDB.chatConversationQueries
        .selectGroupById(
          id = conversationId.value,
          mapper = { _, _, _, memberUids, _, _, _, _, _ -> memberUids }
        )
        .executeAsOneOrNull()
        .orEmpty()
    }
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
      firestore.updateConversationName(conversationId = conversationId.value, name = name)
      persistedDB.chatConversationQueries.updateGroupName(id = conversationId.value, name = name)
    }
  }

  override suspend fun deleteConversation() {
    withContext(Dispatchers.IO) {
      firestore.deleteConversation(conversationId.value)
      persistedDB.transaction {
        persistedDB.chatConversationQueries.deleteById(conversationId.value)
        persistedDB.chatConversationMemberQueries.deleteByConversation(conversationId.value)
      }
    }
  }

  override suspend fun leaveConversation() {
    withContext(Dispatchers.IO) {
      firestore.deleteConversationMember(conversationId.value)
      persistedDB.transaction {
        persistedDB.chatConversationQueries.deleteById(conversationId.value)
        persistedDB.chatConversationMemberQueries.deleteByConversation(conversationId.value)
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
      persistedDB.transaction {
        ids.forEach { member ->
          persistedDB.chatConversationMemberQueries.insertOrReplace(
            conversationId = conversationId.value,
            id = member.value
          )
        }
        val merged = (currentMemberUids() + ids.map { it.value }).distinct()
        persistedDB.chatConversationQueries.updateMemberUids(
          id = conversationId.value,
          memberUids = merged
        )
      }
    }
  }

  override suspend fun deleteConversationMember(id: Member.Id) {
    withContext(Dispatchers.IO) {
      firestore.deleteConversationMember(
        conversationId = conversationId.value,
        memberId = id.value
      )
      persistedDB.transaction {
        persistedDB.chatConversationMemberQueries.deleteByConversationAndId(
          conversationId = conversationId.value,
          id = id.value
        )
        val remaining = currentMemberUids() - id.value
        persistedDB.chatConversationQueries.updateMemberUids(
          id = conversationId.value,
          memberUids = remaining
        )
      }
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
    return persistedDB.chatConversationQueries
      .selectGroupById(
        id = conversationId.value,
        mapper = { rowId, name, ownerUid, memberUids, _, _, _, _, memberCount ->
          Group(
            id = Conversation.Id(rowId),
            name = name.orEmpty(),
            ownerId = UserId(ownerUid.orEmpty()),
            memberCount = memberCount.toInt(),
            memberIds = memberUids.map(::UserId)
          )
        }
      )
      .observeOneOrNull()
  }

  override fun observeGroupMembers(): Flow<List<GroupMember>> = flow {
    val currentUserId = authSessionPersistence.withKey { readUserId(it) }?.value
    val group = observeGroup()
    val members = persistedDB.chatConversationMemberQueries
      .selectByConversationWithEmail(
        conversationId = conversationId.value,
        mapper = { memberId, displayName, email, photoUrl ->
          GroupMember(
            id = UserId(memberId),
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            isOwner = false,
            isMe = memberId == currentUserId
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
    return persistedDB.chatConversationQueries
      .selectGroupById(
        id = conversationId.value,
        mapper = { _, _, _, memberUids, _, _, _, _, _ -> memberUids }
      )
      .executeAsOneOrNull()
      .orEmpty()
  }

  override val commits: Flow<List<Commit>> = flow {
    persistedDB.chatCommitQueries
      .selectByBranchId(conversationId.value, conversationId.value, ::mapToCommit)
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
    persistedDB.transaction {
      changes.forEach { change ->
        when (change.changeType) {
          FirestoreDocumentResult.Removed -> {
            persistedDB.chatCommitQueries.deleteById(change.data.id)
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
    persistedDB.chatCommitQueries.insertOrReplace(
      ChatCommit(
        id = commit.id,
        conversationId = conversationId.value,
        branchId = commit.branchId,
        senderId = commit.senderUid,
        type = commit.type.value,
        text = commit.text.orEmpty(),
        invitedUid = commit.invitedUid,
        createdAtNanos = commit.createdAt?.toEpochNanos() ?: 0L,
        isSelf = commit.senderUid == userId.value,
        status = if (hasPendingWrites) {
          Commit.Status.Sending.value
        } else {
          Commit.Status.Sent.value
        }
      )
    )
  }

  private suspend fun applyMemberChanges(changes: List<FirestoreChange<MemberNM>>) {
    return withContext(Dispatchers.IO) {
      persistedDB.transaction {
        changes.forEach { change ->
          when (change.changeType) {
            FirestoreDocumentResult.Added,
            FirestoreDocumentResult.Modified -> {
              persistedDB.chatConversationMemberQueries.insertOrReplace(
                conversationId = conversationId.value,
                id = change.data.id
              )
            }
            FirestoreDocumentResult.Removed -> {
              persistedDB.chatConversationMemberQueries.deleteByConversationAndId(
                conversationId = conversationId.value,
                id = change.data.id
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
