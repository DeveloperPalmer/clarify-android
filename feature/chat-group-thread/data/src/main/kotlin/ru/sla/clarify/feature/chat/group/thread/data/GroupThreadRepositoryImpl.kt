package ru.sla.clarify.feature.chat.group.thread.data

import androidx.room3.withWriteTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.chat.api.CommitApi
import ru.sla.clarify.chat.api.ConversationApi
import ru.sla.clarify.chat.api.MemberApi
import ru.sla.clarify.chat.api.UnreadCountApi
import ru.sla.clarify.chat.api.UserApi
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.ChatDatabase
import ru.sla.clarify.database.entity.ChatMemberEntity
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.ChatChange
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.CommitRecord
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chat.group.thread.data.mapper.toDomainModel
import ru.sla.clarify.feature.chat.group.thread.domain.GroupThreadRepository
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.domain.entity.FoundUser
import ru.sla.clarify.feature.chat.group.thread.domain.entity.Group
import ru.sla.clarify.feature.chat.group.thread.domain.entity.GroupMember
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams
import ru.sla.clarify.mapper.data.toCacheRow
import ru.sla.clarify.mapper.data.toDomainModel
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
  private val chatDatabase: ChatDatabase,
  private val authSessionPersistence: AuthSessionPersistence
) : GroupThreadRepository {

  private val conversationId = target.conversationId
  private val lastReadWatermark = MutableStateFlow<LocalDateTime?>(null)

  override suspend fun subscribeOnCommitChanges() {
    val userId = requireUserId()
    awaitCachedConversation()
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
    awaitCachedConversation()
    chatDatabase.withWriteTransaction {
      historyCommits.forEach { commit ->
        applyInsertOrReplaceCommit(
          commit = commit,
          userId = userId,
          isPending = false
        )
      }
    }
  }

  override suspend fun sendCommit(text: String) {
    val memberUids = currentMemberUids()
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
    conversationApi.updateConversationName(
      conversationId = conversationId.value,
      name = name
    )
    chatDatabase.chatConversationDao().updateName(
      id = conversationId,
      name = name
    )
  }

  override suspend fun deleteConversation() {
    conversationApi.deleteConversation(conversationId.value)
    deleteCachedConversation()
  }

  override suspend fun leaveConversation() {
    memberApi.deleteConversationMember(conversationId.value)
    deleteCachedConversation()
  }

  override suspend fun inviteGroupMembers(ids: List<Member.Id>) {
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
    chatDatabase.withWriteTransaction {
      ids.forEach { member ->
        chatDatabase.chatMemberDao().insertOrReplace(
          ChatMemberEntity(
            id = member,
            conversationId = conversationId
          )
        )
      }
    }
  }

  override suspend fun deleteConversationMember(id: Member.Id) {
    memberApi.deleteConversationMember(
      conversationId = conversationId.value,
      memberId = id.value
    )
    chatDatabase.chatMemberDao().deleteById(
      conversationId = conversationId,
      id = id
    )
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
    return chatDatabase.chatConversationDao()
      .observeGroup(conversationId)
      .map { it?.toDomainModel() }
  }

  override fun observeGroupMembers(): Flow<List<GroupMember>> = flow {
    val currentUserId = authSessionPersistence.withKey { readUserId(it) }
    val group = observeGroup()
    val members = chatDatabase.chatMemberDao()
      .observeGroup(conversationId)
      .map { rows -> rows.map { it.toDomainModel(currentUserId) } }
    combine(group, members) { groupValue, memberRows ->
      val ownerUid = groupValue?.ownerId?.value
      memberRows.map { member ->
        member.copy(isOwner = member.id.value == ownerUid)
      }
    }.collect { emit(it) }
  }

  private suspend fun currentMemberUids(): List<String> {
    return chatDatabase.chatMemberDao()
      .selectIds(conversationId)
      .map { it.value }
  }

  private suspend fun deleteCachedConversation() {
    chatDatabase.withWriteTransaction {
      chatDatabase.chatConversationDao().delete(conversationId)
      chatDatabase.chatMemberDao().delete(conversationId)
    }
  }

  override val commits: Flow<List<Commit>> = flow {
    chatDatabase.chatCommitDao()
      .observe(conversationId, Branch.Id(conversationId.value))
      .map { rows -> rows.map { it.toDomainModel() } }
      .collect { emit(it) }
  }

  override val unreadCount: Flow<Long> = flow {
    unreadCountApi.unreadCountLive(conversationId.value)
      .collect { emit(it) }
  }

  private suspend fun applyCommitChanges(
    userId: UserId,
    changes: List<ChatChange<CommitRecord>>
  ) {
    chatDatabase.withWriteTransaction {
      changes.forEach { change ->
        when (change.changeType) {
          ChatChange.Type.Removed -> {
            chatDatabase.chatCommitDao().delete(change.data.id)
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

  private suspend fun applyInsertOrReplaceCommit(
    commit: CommitRecord,
    userId: UserId,
    isPending: Boolean
  ) {
    val row = commit.toCacheRow(
      conversationId = conversationId,
      selfUserId = userId,
      isPending = isPending
    )
    chatDatabase.chatCommitDao().insertOrReplaceIfConversationExists(row)
  }

  /**
   * Коммит ссылается на разговор внешним ключом, поэтому писать ленту можно, только когда группа
   * уже есть в кэше. Тред может открыться раньше, чем список разговоров её туда положит.
   */
  private suspend fun awaitCachedConversation() {
    chatDatabase.chatConversationDao()
      .observeGroup(conversationId)
      .filterNotNull()
      .first()
  }

  private suspend fun applyMemberChanges(changes: List<ChatChange<Member.Id>>) {
    chatDatabase.withWriteTransaction {
      changes.forEach { change ->
        when (change.changeType) {
          ChatChange.Type.Added,
          ChatChange.Type.Modified -> {
            chatDatabase.chatMemberDao().insertOrReplace(
              ChatMemberEntity(
                id = change.data,
                conversationId = conversationId
              )
            )
          }
          ChatChange.Type.Removed -> {
            chatDatabase.chatMemberDao().deleteById(
              conversationId = conversationId,
              id = change.data
            )
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
