package ru.sla.clarify.feature.chat.group.thread.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.chat.api.CommitApi
import ru.sla.clarify.chat.api.ConversationApi
import ru.sla.clarify.chat.api.MemberApi
import ru.sla.clarify.chat.api.UnreadCountApi
import ru.sla.clarify.chat.api.UserApi
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.ChatChange
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.CommitRecord
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chat.group.thread.domain.GroupThreadRepository
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.domain.entity.FoundUser
import ru.sla.clarify.feature.chat.group.thread.domain.entity.Group
import ru.sla.clarify.feature.chat.group.thread.domain.entity.GroupMember
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams
import ru.sla.clarify.mapper.data.mapToCommit
import ru.sla.clarify.mapper.data.toCacheRow
import ru.sla.log.log
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.time.LocalDateTime

@SingleIn(GroupThreadScope::class)
@ContributesBinding(GroupThreadScope::class)
class GroupThreadRepositoryImpl @Inject constructor(
  target: TargetParams,
  private val commitApi: CommitApi,
  private val conversationApi: ConversationApi,
  private val memberApi: MemberApi,
  private val unreadCountApi: UnreadCountApi,
  private val userApi: UserApi,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) : GroupThreadRepository {

  private val conversationId = target.conversationId
  private val lastReadWatermark = MutableStateFlow<LocalDateTime?>(null)

  override suspend fun subscribeOnCommitChanges() {
    val userId = requireUserId()
    commitApi.groupCommitsLive(
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
    val historyCommits = commitApi.readCommits(
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
            isPending = false
          )
        }
      }
    }
  }

  override suspend fun sendCommit(text: String) {
    val memberUids = withContext(Dispatchers.IO) { currentMemberUids() }
    commitApi.createGroupCommit(
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
    memberApi.updateReadWatermark(
      conversationId = conversationId.value,
      lastReadAt = lastReadAt
    )
    unreadCountApi.updateUnreadCount(
      conversationId = conversationId.value
    )
  }

  override suspend fun subscribeOnGroupMembers() {
    memberApi.membersLive(conversationId.value)
      .collect(::applyMemberChanges)
  }

  override suspend fun renameGroup(name: String) {
    withContext(Dispatchers.IO) {
      conversationApi.updateConversationName(
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
      conversationApi.deleteConversation(conversationId.value)
      inMemoryDB.transaction {
        inMemoryDB.chatConversationQueries.delete(conversationId)
        inMemoryDB.chatMemberQueries.delete(conversationId)
      }
    }
  }

  override suspend fun leaveConversation() {
    withContext(Dispatchers.IO) {
      memberApi.deleteConversationMember(conversationId.value)
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
        memberApi.createCommitInviteMember(
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
      memberApi.deleteConversationMember(
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
    return userApi.readUsersByEmailPrefix(
      prefix = prefix.lowercase(),
      limit = USER_SEARCH_LIMIT
    ).map { user ->
      FoundUser(
        id = user.id,
        displayName = user.displayName,
        email = user.email.value,
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
    unreadCountApi.unreadCountLive(conversationId.value)
      .collect { emit(it) }
  }

  private suspend fun applyCommitChanges(
    userId: UserId,
    changes: List<ChatChange<CommitRecord>>
  ): Unit = withContext(Dispatchers.IO) {
    inMemoryDB.transaction {
      changes.forEach { change ->
        when (change.changeType) {
          ChatChange.Type.Removed -> {
            inMemoryDB.chatCommitQueries.delete(change.data.id)
          }
          ChatChange.Type.Added,
          ChatChange.Type.Modified -> {
            applyInsertOrReplaceCommit(
              commit = change.data,
              userId = userId,
              isPending = change.isPending
            )
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceCommit(
    commit: CommitRecord,
    userId: UserId,
    isPending: Boolean
  ) {
    val row = commit.toCacheRow(
      conversationId = conversationId,
      selfUserId = userId,
      isPending = isPending
    )
    inMemoryDB.chatCommitQueries.insertOrReplace(
      id = row.id,
      conversationId = row.conversationId,
      branchId = row.branchId,
      senderId = row.senderId,
      type = row.type,
      text = row.text,
      replyCommit = row.replyCommit,
      invitedId = row.invitedId,
      createdAtNanos = row.createdAtNanos,
      isSelf = row.isSelf,
      status = row.status,
      editedAtNanos = row.editedAtNanos
    )
  }

  private suspend fun applyMemberChanges(changes: List<ChatChange<Member.Id>>) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        changes.forEach { change ->
          when (change.changeType) {
            ChatChange.Type.Added,
            ChatChange.Type.Modified -> {
              inMemoryDB.chatMemberQueries.insertOrReplace(
                conversationId = conversationId,
                id = change.data
              )
            }
            ChatChange.Type.Removed -> {
              inMemoryDB.chatMemberQueries.deleteById(
                id = change.data,
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
