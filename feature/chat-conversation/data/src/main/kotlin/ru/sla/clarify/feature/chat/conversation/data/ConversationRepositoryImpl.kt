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
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.conversation.data.mapper.mapToConversation
import ru.sla.clarify.feature.chat.conversation.data.mapper.mapToGroup
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.entity.PeerNotFoundException
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.toEpochSeconds
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
    val userId = authSessionPersistence.withKey { readUserId(it) } ?: return
    inMemoryDB.chatMemberQueries
      .selectMembersWithoutProfile(userId.value)
      .observeList()
      .collect(::applyMemberProfiles)
  }

  override suspend fun subscribeOnConversationsUnreadCounts() {
    inMemoryDB.chatConversationQueries
      .selectAllIds()
      .observeList()
      .collectLatest(::subscribeOnConversationsUnreadCounts)
  }

  override suspend fun fetchCurrentUser() {
    return withContext(Dispatchers.IO) {
      val firestoreUser = firestore.readCurrentUser()
      inMemoryDB.userQueries.insertOrReplace(
        id = firestoreUser.id,
        email = firestoreUser.email,
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
      val conversationId = firestore.createGroupConversation(name)
      val ownerId = authSessionPersistence.withKey { readUserId(it) }
      if (ownerId != null) {
        inMemoryDB.transaction {
          inMemoryDB.chatConversationQueries.insertOrReplaceMeta(
            id = conversationId,
            type = ConversationNM.Type.Group.value,
            name = name.value,
            ownerUid = ownerId.value,
            lastCommit = null,
            lastCommitSenderUid = null,
            lastCommitTimestamp = 0L
          )
          inMemoryDB.chatMemberQueries.insertOrReplace(
            conversationId = conversationId,
            id = ownerId.value
          )
        }
      }
      Conversation.Id(conversationId)
    }
  }

  override suspend fun deleteConversations(ids: List<Conversation.Id>) {
    return withContext(Dispatchers.IO) {
      val deletableIds = ids.map { it.value }
      firestore.deleteConversations(deletableIds)
      inMemoryDB.transaction {
        deletableIds.forEach {
          inMemoryDB.chatConversationQueries.deleteById(it)
          inMemoryDB.chatMemberQueries.deleteByConversation(it)
        }
      }
    }
  }

  override val user: Flow<User?> = flow {
    val userId = authSessionPersistence.withKey { readUserId(it) }
    if (userId == null) return@flow emit(null)

    inMemoryDB.userQueries
      .selectById(userId.value, ::mapToUser)
      .observeOneOrNull()
      .collect { emit(it) }
  }

  override val conversations: Flow<List<Conversation>> = flow {
    val userId = authSessionPersistence.withKey { readUserId(it) }
    if (userId == null) return@flow emit(emptyList())

    val directs = inMemoryDB.chatConversationQueries
      .selectAllWithPeer(userId.value, ::mapToConversation)
      .observeList()

    val groups = inMemoryDB.chatConversationQueries
      .selectAllGroups(::mapToGroup)
      .observeList()

    combine(
      flow = directs,
      flow2 = groups
    ) { directs, groups ->
      (directs + groups).sortedByDescending { it.lastCommitTimestamp }
    }.collect { emit(it) }
  }

  private suspend fun applyMemberProfiles(ids: List<String>) = coroutineScope {
    ids.forEach { memberId ->
      launch { applyInsertOrReplaceUsers(memberId) }
    }
  }

  private suspend fun subscribeOnConversationsUnreadCounts(ids: List<String>) {
    return coroutineScope {
      ids.forEach { conversationId ->
        launch {
          firestore.unreadCountLive(
            conversationId = conversationId
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

  private suspend fun applyConversationsChanges(changes: List<FirestoreChange<ConversationNM>>) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.transaction {
        changes.forEach { change ->
          when (change.changeType) {
            FirestoreDocumentResult.Added,
            FirestoreDocumentResult.Modified -> {
              applyConversationChanges(change.data)
            }
            FirestoreDocumentResult.Removed -> {
              inMemoryDB.chatConversationQueries.deleteById(change.data.id)
              inMemoryDB.chatMemberQueries.deleteByConversation(change.data.id)
            }
          }
        }
      }
    }
  }

  private fun applyConversationChanges(conversation: ConversationNM) {
    applyMembers(
      conversationId = conversation.id,
      memberUids = conversation.memberUids
    )
    inMemoryDB.chatConversationQueries.insertOrReplaceMeta(
      id = conversation.id,
      type = conversation.type.value,
      name = conversation.name,
      ownerUid = conversation.ownerUid,
      lastCommit = conversation.lastCommitText,
      lastCommitSenderUid = conversation.lastCommitSenderUid,
      lastCommitTimestamp = conversation.lastCommitAt?.toEpochSeconds() ?: 0L
    )
  }

  /**
   * Приводит состав участников беседы к тому, что пришёл в документе: `memberUids` —
   * полный список, поэтому исключённых нужно убрать, а не только добавить новых.
   */
  private fun applyMembers(conversationId: String, memberUids: List<String>) {
    if (memberUids.isEmpty()) {
      inMemoryDB.chatMemberQueries.deleteByConversation(conversationId)
      return
    }
    inMemoryDB.chatMemberQueries.deleteByConversationExcept(
      conversationId = conversationId,
      memberIds = memberUids
    )
    memberUids.forEach { memberId ->
      inMemoryDB.chatMemberQueries.insertOrReplace(
        conversationId = conversationId,
        id = memberId
      )
    }
  }

  private suspend fun applyInsertOrReplaceUsers(memberId: String) {
    val profile = firestore.readUser(UserId(memberId)) ?: return
    return withContext(Dispatchers.IO) {
      inMemoryDB.userQueries.insertOrReplace(
        id = profile.id,
        email = profile.email,
        displayName = profile.displayName,
        photoUrl = profile.photoUrl
      )
    }
  }

  private suspend fun applyUpdateUnreadCount(conversationId: String, unreadCount: Long) {
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatConversationQueries.updateUnreadCount(
        id = conversationId,
        unreadCount = unreadCount
      )
    }
  }
}
