package ru.sla.clarify.feature.chat.group.thread.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.feature.chat.group.thread.data.mapper.mapToCommit
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
import ru.sla.clarify.lib.google.firestore.toEpochSeconds
import java.time.LocalDateTime
import javax.inject.Inject

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
    firestore.observeGroupCommits(
      conversationId = conversationId.value,
      limit = LIVE_COMMIT_LIMIT
    ).flowOn(
      context = Dispatchers.IO
    ).collect { changes ->
      applyCommitChanges(userId = userId, changes = changes)
    }
  }

  override suspend fun fetchHistoryCommits(count: Int, before: Commit?) {
    val historyCommits = firestore.getCommits(
      conversationId = conversationId.value,
      branchId = conversationId.value,
      count = count,
      before = before?.timestamp
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
    val memberUids = persistedDB.chatConversationQueries
      .selectGroupById(
        id = conversationId.value,
        mapper = { _, _, _, memberUids, _, _, _, _, _ -> memberUids }
      )
      .executeAsOneOrNull()
      .orEmpty()
    firestore.postGroupCommit(
      conversationId = conversationId.value,
      text = text,
      memberUids = memberUids
    )
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    val current = lastReadWatermark.value
    if (current != null && !lastReadAt.isAfter(current)) {
      return
    }
    lastReadWatermark.value = lastReadAt
    firestore.patchReadWatermark(
      conversationId = conversationId.value,
      lastReadAt = lastReadAt
    )
    firestore.patchClearUnreadCount(
      conversationId = conversationId.value
    )
  }

  override suspend fun subscribeOnGroupMembers() {
    firestore.observeMembers(conversationId.value)
      .flowOn(Dispatchers.IO)
      .collect { changes ->
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

  override suspend fun renameGroup(name: String) {
    withContext(Dispatchers.IO) {
      firestore.patchGroupName(conversationId = conversationId.value, name = name)
      persistedDB.chatConversationQueries.updateGroupName(id = conversationId.value, name = name)
    }
  }

  override suspend fun deleteGroup() {
    withContext(Dispatchers.IO) {
      firestore.deleteGroupConversation(conversationId.value)
      persistedDB.transaction {
        persistedDB.chatConversationQueries.deleteById(conversationId.value)
        persistedDB.chatConversationMemberQueries.deleteByConversation(conversationId.value)
      }
    }
  }

  override suspend fun leaveGroup() {
    withContext(Dispatchers.IO) {
      firestore.leaveGroup(conversationId.value)
      persistedDB.transaction {
        persistedDB.chatConversationQueries.deleteById(conversationId.value)
        persistedDB.chatConversationMemberQueries.deleteByConversation(conversationId.value)
      }
    }
  }

  override suspend fun inviteGroupMembers(userIds: List<UserId>) {
    withContext(Dispatchers.IO) {
      userIds.forEach { userId ->
        firestore.postInviteMember(
          conversationId = conversationId.value,
          invitedUserId = userId
        )
      }
      // Локально добавляем участников и обновляем денормализованный memberUids,
      // из которого sendCommit берёт получателей unread-инкрементов. Иначе колонка
      // отстаёт до прихода observeMembers-синка.
      persistedDB.transaction {
        userIds.forEach { userId ->
          persistedDB.chatConversationMemberQueries.insertOrReplace(
            conversationId = conversationId.value,
            id = userId.value
          )
        }
        val merged = (currentMemberUids() + userIds.map { it.value }).distinct()
        persistedDB.chatConversationQueries.updateMemberUids(
          id = conversationId.value,
          memberUids = merged
        )
      }
    }
  }

  override suspend fun removeGroupMember(userId: UserId) {
    withContext(Dispatchers.IO) {
      firestore.deleteMember(conversationId = conversationId.value, userId = userId)
      persistedDB.transaction {
        persistedDB.chatConversationMemberQueries.deleteByConversationAndId(
          conversationId = conversationId.value,
          id = userId.value
        )
        val remaining = currentMemberUids() - userId.value
        persistedDB.chatConversationQueries.updateMemberUids(
          id = conversationId.value,
          memberUids = remaining
        )
      }
    }
  }

  override suspend fun searchMemberByPrefix(prefix: String): List<FoundUser> {
    return withContext(Dispatchers.IO) {
      firestore.getUsersByEmailPrefix(prefix = prefix.lowercase(), limit = USER_SEARCH_LIMIT)
        .map { user ->
          FoundUser(
            id = UserId(user.id),
            displayName = user.displayName,
            email = user.email,
            photoUrl = user.photoUrl
          )
        }
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
    firestore.observeUnreadCount(conversationId.value)
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
      id = commit.id,
      conversationId = conversationId.value,
      branchId = commit.branchId,
      senderId = commit.senderUid,
      type = commit.type.value,
      text = commit.text.orEmpty(),
      invitedUid = commit.invitedUid,
      timestamp = commit.createdAt?.toEpochSeconds() ?: 0L,
      isSelf = commit.senderUid == userId.value,
      status = if (hasPendingWrites) {
        Commit.Status.Sending.value
      } else {
        Commit.Status.Sent.value
      }
    )
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) })
  }
}

private const val LIVE_COMMIT_LIMIT = 30L
private const val USER_SEARCH_LIMIT = 10L
