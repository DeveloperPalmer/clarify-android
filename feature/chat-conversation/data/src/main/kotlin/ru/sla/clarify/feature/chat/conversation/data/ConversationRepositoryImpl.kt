package ru.sla.clarify.feature.chat.conversation.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.GroupName
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.feature.chat.conversation.data.mapper.mapToConversation
import ru.sla.clarify.feature.chat.conversation.data.mapper.mapToGroup
import ru.sla.clarify.feature.chat.conversation.data.mapper.mapToUser
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.entity.PeerNotFoundException
import ru.sla.clarify.feature.entity.chat.Conversation
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.toEpochSeconds
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ConversationRepositoryImpl @Inject constructor(
  private val firestore: Firestore,
  private val persistedDB: PersistedDB,
  private val authSessionPersistence: AuthSessionPersistence
) : ConversationRepository {

  override suspend fun subscribeOnConversations() {
    firestore.observeConversations()
      .flowOn(Dispatchers.IO)
      .collect(::applyConversationsChanges)
  }

  override suspend fun subscribeOnMemberProfiles() {
    val userId = authSessionPersistence.withKey { readUserId(it) } ?: return
    persistedDB.chatConversationMemberQueries
      .selectMembersWithoutProfile(userId.value)
      .observeList()
      .collect(::applyMemberProfiles)
  }

  override suspend fun subscribeOnConversationsUnreadCounts() {
    persistedDB.chatConversationQueries
      .selectAllIds()
      .observeList()
      .collectLatest(::subscribeOnConversationsUnreadCounts)
  }

  override suspend fun fetchCurrentUser() {
    return withContext(Dispatchers.IO) {
      val firestoreUser = firestore.getCurrentUser()
      persistedDB.userQueries.insertOrReplace(
        id = firestoreUser.id,
        email = firestoreUser.email,
        displayName = firestoreUser.displayName,
        photoUrl = firestoreUser.photoUrl
      )
    }
  }

  override suspend fun getPeerByEmail(email: Email): Peer.Id {
    return withContext(Dispatchers.IO) {
      val userId = firestore.getUserIdByEmail(email) ?: throw PeerNotFoundException(email)
      Peer.Id(userId.value)
    }
  }

  override suspend fun createGroup(name: GroupName): Conversation.Id {
    return withContext(Dispatchers.IO) {
      val conversationId = firestore.postGroupConversation(name)
      val ownerId = authSessionPersistence.withKey { readUserId(it) }
      if (ownerId != null) {
        persistedDB.transaction {
          persistedDB.chatConversationQueries.insertOrReplaceMeta(
            id = conversationId,
            type = ConversationNM.Type.Group.value,
            memberUids = listOf(ownerId.value),
            name = name.value,
            ownerUid = ownerId.value,
            lastCommit = null,
            lastCommitSenderUid = null,
            lastCommitTimestamp = 0L
          )
          persistedDB.chatConversationMemberQueries.insertOrReplace(
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
      persistedDB.transaction {
        deletableIds.forEach {
          persistedDB.chatConversationQueries.deleteById(it)
          persistedDB.chatConversationMemberQueries.deleteByConversation(it)
        }
      }
    }
  }

  override val user: Flow<User?> = flow {
    val userId = authSessionPersistence.withKey { readUserId(it) }
    if (userId == null) return@flow emit(null)

    persistedDB.userQueries
      .selectById(userId.value, ::mapToUser)
      .observeOneOrNull()
      .collect { emit(it) }
  }

  override val conversations: Flow<List<Conversation>> = flow {
    val userId = authSessionPersistence.withKey { readUserId(it) }
    if (userId == null) return@flow emit(emptyList())

    val directs = persistedDB.chatConversationQueries
      .selectAllWithPeer(userId.value, ::mapToConversation)
      .observeList()

    val groups = persistedDB.chatConversationQueries
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
          firestore.observeUnreadCount(
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

  private fun applyConversationsChanges(changes: List<FirestoreChange<ConversationNM>>) {
    persistedDB.transaction {
      changes.forEach { change ->
        when (change.changeType) {
          FirestoreDocumentResult.Added -> {
            applyConversationChanges(change.data)
          }
          FirestoreDocumentResult.Modified -> {
            applyInsertOrReplaceMetaConversation(change.data)
          }
          FirestoreDocumentResult.Removed -> {
            persistedDB.chatConversationQueries.deleteById(change.data.id)
            persistedDB.chatConversationMemberQueries.deleteByConversation(change.data.id)
          }
        }
      }
    }
  }

  private fun applyConversationChanges(conversation: ConversationNM) {
    conversation.memberUids.forEach { memberId ->
      persistedDB.chatConversationMemberQueries.insertOrReplace(
        conversationId = conversation.id,
        id = memberId
      )
    }
    persistedDB.chatConversationQueries.insertOrReplaceMeta(
      id = conversation.id,
      type = conversation.type.value,
      memberUids = conversation.memberUids,
      name = conversation.name,
      ownerUid = conversation.ownerUid,
      lastCommit = conversation.lastCommitText,
      lastCommitSenderUid = conversation.lastCommitSenderUid,
      lastCommitTimestamp = conversation.lastCommitAt?.toEpochSeconds() ?: 0L
    )
  }

  private suspend fun applyInsertOrReplaceUsers(userId: String) {
    val profile = firestore.getUser(UserId(userId)) ?: return
    persistedDB.userQueries.insertOrReplace(
      id = profile.id,
      email = profile.email,
      displayName = profile.displayName,
      photoUrl = profile.photoUrl
    )
  }

  private fun applyInsertOrReplaceMetaConversation(conversationNM: ConversationNM) {
    persistedDB.chatConversationQueries.insertOrReplaceMeta(
      id = conversationNM.id,
      type = conversationNM.type.value,
      memberUids = conversationNM.memberUids,
      name = conversationNM.name,
      ownerUid = conversationNM.ownerUid,
      lastCommit = conversationNM.lastCommitText,
      lastCommitSenderUid = conversationNM.lastCommitSenderUid,
      lastCommitTimestamp = conversationNM.lastCommitAt?.toEpochSeconds() ?: 0L
    )
  }

  private fun applyUpdateUnreadCount(conversationId: String, unreadCount: Long) {
    persistedDB.chatConversationQueries.updateUnreadCount(
      id = conversationId,
      unreadCount = unreadCount
    )
  }
}
