package ru.sla.clarify.feature.chat.conversation.data

import androidx.room3.withWriteTransaction
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.chat.api.ConversationApi
import ru.sla.clarify.chat.api.UnreadCountApi
import ru.sla.clarify.chat.api.UserApi
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.GroupName
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.ChatDatabase
import ru.sla.clarify.database.entity.ChatMemberEntity
import ru.sla.clarify.entity.chat.ChatChange
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.ConversationRecord
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.conversation.data.mapper.toDomainModel
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.entity.PeerNotFoundException
import ru.sla.clarify.mapper.data.toCacheRow
import ru.sla.clarify.mapper.data.toDomainModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ConversationRepositoryImpl @Inject constructor(
  private val conversationApi: ConversationApi,
  private val unreadCountApi: UnreadCountApi,
  private val userApi: UserApi,
  private val chatDatabase: ChatDatabase,
  private val authSessionPersistence: AuthSessionPersistence
) : ConversationRepository {

  override suspend fun subscribeOnConversations() {
    conversationApi.conversationsLive()
      .collect(::applyConversationsChanges)
  }

  override suspend fun subscribeOnMemberProfiles() {
    val userId = findUserId() ?: return
    chatDatabase.chatMemberDao()
      .observeWithoutProfile(Member.Id(userId.value))
      .collect(::applyMemberProfiles)
  }

  override suspend fun subscribeOnConversationsUnreadCounts() {
    chatDatabase.chatConversationDao()
      .observeIds()
      .collectLatest(::subscribeOnConversationsUnreadCounts)
  }

  override suspend fun fetchCurrentUser() {
    val currentUser = userApi.readCurrentUser()
    chatDatabase.userDao().insertOrReplace(currentUser.toCacheRow())
  }

  override suspend fun getPeerByEmail(email: Email): Peer.Id {
    val userId = userApi.readUserIdByEmail(email) ?: throw PeerNotFoundException(email)
    return Peer.Id(userId.value)
  }

  override suspend fun createGroupConversation(name: GroupName): Conversation.Id {
    val userId = findUserId()
    val conversationId = Conversation.Id(conversationApi.createGroupConversation(name))
    if (userId != null) {
      chatDatabase.withWriteTransaction {
        chatDatabase.chatConversationDao().upsert(
          id = conversationId,
          type = ConversationRecord.Type.Group.value,
          name = name.value,
          ownerId = userId,
          lastCommit = null,
          lastCommitSenderId = null,
          lastCommitTimestamp = 0L
        )
        chatDatabase.chatMemberDao().insertOrReplace(
          ChatMemberEntity(
            id = Member.Id(userId.value),
            conversationId = conversationId
          )
        )
      }
    }
    return conversationId
  }

  override suspend fun deleteConversations(ids: List<Conversation.Id>) {
    conversationApi.deleteConversations(ids.map { it.value })
    chatDatabase.withWriteTransaction {
      ids.forEach {
        chatDatabase.chatConversationDao().delete(it)
        chatDatabase.chatMemberDao().delete(it)
      }
    }
  }

  override val user: Flow<User?> = flow {
    val userId = findUserId() ?: return@flow emit(null)
    chatDatabase.userDao()
      .observe(userId)
      .map { it?.toDomainModel() }
      .collect { emit(it) }
  }

  override val conversations: Flow<List<Conversation>> = flow {
    val userId = findUserId() ?: return@flow emit(emptyList())

    val directs = chatDatabase.chatConversationDao()
      .observeDirects(Member.Id(userId.value))
      .map { rows -> rows.map { it.toDomainModel() } }

    val groups = chatDatabase.chatConversationDao()
      .observeGroups()
      .map { rows -> rows.map { it.toDomainModel() } }

    combine(
      flow = directs,
      flow2 = groups
    ) { directs, groups ->
      (directs + groups).sortedByDescending { it.lastCommitTimestamp }
    }.collect { emit(it) }
  }

  private suspend fun applyMemberProfiles(ids: List<Member.Id>) {
    return coroutineScope {
      ids.forEach { memberId -> launch { applyInsertOrReplace(memberId) } }
    }
  }

  private suspend fun subscribeOnConversationsUnreadCounts(ids: List<Conversation.Id>) {
    return coroutineScope {
      ids.forEach { conversationId ->
        launch {
          unreadCountApi.unreadCountLive(
            conversationId = conversationId.value
          ).collect { unreadCount ->
            applyUpdateUnreadCount(
              conversationId = conversationId,
              unreadCount = unreadCount
            )
          }
        }
      }
    }
  }

  private suspend fun applyConversationsChanges(changes: List<ChatChange<ConversationRecord>>) {
    chatDatabase.withWriteTransaction {
      changes.forEach { change ->
        when (change.changeType) {
          ChatChange.Type.Added,
          ChatChange.Type.Modified -> {
            applyConversationChanges(change.data)
          }
          ChatChange.Type.Removed -> {
            val conversationId = change.data.id
            chatDatabase.chatConversationDao().delete(conversationId)
            chatDatabase.chatMemberDao().delete(conversationId)
          }
        }
      }
    }
  }

  private suspend fun applyConversationChanges(conversation: ConversationRecord) {
    applyMembers(
      conversationId = conversation.id,
      memberIds = conversation.memberIds
    )
    chatDatabase.chatConversationDao().upsert(
      id = conversation.id,
      type = conversation.type.value,
      name = conversation.name,
      ownerId = conversation.ownerId,
      lastCommit = conversation.lastCommitText,
      lastCommitSenderId = conversation.lastCommitSenderId,
      lastCommitTimestamp = conversation.lastCommitAtSeconds
    )
  }

  private suspend fun applyMembers(conversationId: Conversation.Id, memberIds: List<Member.Id>) {
    if (memberIds.isEmpty()) {
      chatDatabase.chatMemberDao().delete(conversationId)
      return
    }

    chatDatabase.chatMemberDao().deleteExcept(
      conversationId = conversationId,
      memberIds = memberIds
    )
    memberIds.forEach { memberId ->
      chatDatabase.chatMemberDao().insertOrReplace(
        ChatMemberEntity(
          id = memberId,
          conversationId = conversationId
        )
      )
    }
  }

  private suspend fun applyInsertOrReplace(memberId: Member.Id) {
    val user = userApi.readUser(UserId(memberId.value)) ?: return
    chatDatabase.userDao().insertOrReplace(user.toCacheRow())
  }

  private suspend fun applyUpdateUnreadCount(conversationId: Conversation.Id, unreadCount: Long) {
    chatDatabase.chatConversationDao().updateUnreadCount(
      id = conversationId,
      unreadCount = unreadCount
    )
  }

  private suspend fun findUserId(): UserId? {
    return authSessionPersistence.withKey { readUserId(it) }
  }
}
