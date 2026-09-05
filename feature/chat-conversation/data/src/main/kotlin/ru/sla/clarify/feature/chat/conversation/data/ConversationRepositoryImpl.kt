package ru.sla.clarify.feature.chat.conversation.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.GroupName
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.ChatChange
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.ConversationRecord
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.conversation.data.mapper.mapToConversation
import ru.sla.clarify.feature.chat.conversation.data.mapper.mapToGroup
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.entity.PeerNotFoundException
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.mapper.data.mapToUser
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ConversationRepositoryImpl @Inject constructor(
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) : ConversationRepository {

  override suspend fun subscribeOnConversations() {
    firestore.conversationsLive()
      .collect(::applyConversationsChanges)
  }

  override suspend fun subscribeOnMemberProfiles() {
    val userId = findUserId() ?: return
    inMemoryDB.chatMemberQueries
      .selectWithoutProfile(Member.Id(userId.value))
      .observeList()
      .collect(::applyMemberProfiles)
  }

  override suspend fun subscribeOnConversationsUnreadCounts() {
    inMemoryDB.chatConversationQueries
      .selectIds()
      .observeList()
      .collectLatest(::subscribeOnConversationsUnreadCounts)
  }

  override suspend fun fetchCurrentUser() {
    return withContext(Dispatchers.IO) {
      val firestoreUser = firestore.readCurrentUser()
      inMemoryDB.userQueries.insertOrReplace(
        id = firestoreUser.id,
        email = firestoreUser.email.value,
        displayName = firestoreUser.displayName,
        photoUrl = firestoreUser.photoUrl
      )
    }
  }

  override suspend fun getPeerByEmail(email: Email): Peer.Id {
    val userId = firestore.readUserIdByEmail(email) ?: throw PeerNotFoundException(email)
    return Peer.Id(userId.value)
  }

  override suspend fun createGroupConversation(name: GroupName): Conversation.Id {
    return withContext(Dispatchers.IO) {
      val userId = findUserId()
      val conversationId = Conversation.Id(firestore.createGroupConversation(name))
      if (userId != null) {
        inMemoryDB.transaction {
          inMemoryDB.chatConversationQueries.insertOrReplace(
            id = conversationId,
            type = Conversation.Type.Group.value,
            name = name.value,
            ownerId = userId,
            lastCommit = null,
            lastCommitSenderId = null,
            lastCommitTimestamp = 0L
          )
          inMemoryDB.chatMemberQueries.insertOrReplace(
            id = Member.Id(userId.value),
            conversationId = conversationId
          )
        }
      }
      conversationId
    }
  }

  override suspend fun deleteConversations(ids: List<Conversation.Id>) {
    return withContext(Dispatchers.IO) {
      firestore.deleteConversations(ids.map { it.value })
      inMemoryDB.transaction {
        ids.forEach {
          inMemoryDB.chatConversationQueries.delete(it)
          inMemoryDB.chatMemberQueries.delete(it)
        }
      }
    }
  }

  override val user: Flow<User?> = flow {
    val userId = findUserId() ?: return@flow emit(null)
    inMemoryDB.userQueries
      .select(userId, ::mapToUser)
      .observeOneOrNull()
      .collect { emit(it) }
  }

  override val conversations: Flow<List<Conversation>> = flow {
    val userId = findUserId() ?: return@flow emit(emptyList())

    val directs = inMemoryDB.chatConversationQueries
      .selectDirects(Member.Id(userId.value), ::mapToConversation)
      .observeList()

    val groups = inMemoryDB.chatConversationQueries
      .selectGroups(::mapToGroup)
      .observeList()

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
          firestore.unreadCountLive(
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
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        changes.forEach { change ->
          when (change.changeType) {
            ChatChange.Type.Added,
            ChatChange.Type.Modified -> {
              applyConversationChanges(change.data)
            }
            ChatChange.Type.Removed -> {
              val conversationId = change.data.id
              inMemoryDB.chatConversationQueries.delete(conversationId)
              inMemoryDB.chatMemberQueries.delete(conversationId)
            }
          }
        }
      }
    }
  }

  private fun applyConversationChanges(conversation: ConversationRecord) {
    applyMembers(
      conversationId = conversation.id,
      memberIds = conversation.memberIds
    )
    inMemoryDB.chatConversationQueries.insertOrReplace(
      id = conversation.id,
      type = conversation.type.value,
      name = conversation.name,
      ownerId = conversation.ownerId,
      lastCommit = conversation.lastCommitText,
      lastCommitSenderId = conversation.lastCommitSenderId,
      lastCommitTimestamp = conversation.lastCommitAtSeconds
    )
  }

  private fun applyMembers(conversationId: Conversation.Id, memberIds: List<Member.Id>) {
    if (memberIds.isEmpty()) {
      inMemoryDB.chatMemberQueries.delete(conversationId)
      return
    }

    inMemoryDB.chatMemberQueries.deleteExcept(
      conversationId = conversationId,
      memberIds = memberIds
    )
    memberIds.forEach { memberId ->
      inMemoryDB.chatMemberQueries.insertOrReplace(
        conversationId = conversationId,
        id = memberId
      )
    }
  }

  private suspend fun applyInsertOrReplace(memberId: Member.Id) {
    val user = firestore.readUser(UserId(memberId.value)) ?: return
    return withContext(Dispatchers.IO) {
      inMemoryDB.userQueries.insertOrReplace(
        id = user.id,
        email = user.email.value,
        displayName = user.displayName,
        photoUrl = user.photoUrl
      )
    }
  }

  private suspend fun applyUpdateUnreadCount(conversationId: Conversation.Id, unreadCount: Long) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatConversationQueries.updateUnreadCount(
        id = conversationId,
        unreadCount = unreadCount
      )
    }
  }

  private suspend fun findUserId(): UserId? {
    return authSessionPersistence.withKey { readUserId(it) }
  }
}
