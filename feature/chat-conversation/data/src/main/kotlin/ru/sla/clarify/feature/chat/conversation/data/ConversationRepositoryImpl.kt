package ru.sla.clarify.feature.chat.conversation.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.AppScope
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.feature.chat.conversation.data.mapper.selectAll
import ru.sla.clarify.feature.chat.conversation.data.mapper.toDomain
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.PeerNotFoundException
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.conversation.domain.entity.Participant
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

  override val user: Flow<User?> = userId().flatMapLatest { userId ->
    persistedDB.userQueries
      .selectById(userId.value)
      .observeOneOrNull()
      .map { user -> user?.toDomain() }
  }

  override suspend fun fetchCurrentUser(): Unit = withContext(Dispatchers.IO) {
    val firestoreUser = firestore.getCurrentUser()
    persistedDB.userQueries.insertOrReplace(
      id = firestoreUser.id,
      email = firestoreUser.email,
      displayName = firestoreUser.displayName,
      photoUrl = firestoreUser.photoUrl
    )
  }

  override suspend fun getPeerByEmail(email: Email): Peer.Id = withContext(Dispatchers.IO) {
    val userId = firestore.getUserIdByEmail(email) ?: throw PeerNotFoundException(email)
    return@withContext Peer.Id(userId.value)
  }

  override val conversations: Flow<List<Conversation>> = userId().flatMapLatest { userId ->
    persistedDB.chatConversationQueries
      .selectAll(userId)
      .observeList()
  }

  override fun subscribeOnConversations(): Flow<Unit> {
    return firestore.conversationsLive()
      .map { changes -> changes.forEach { applyChange(it) } }
      .flowOn(Dispatchers.IO)
  }

  override suspend fun deleteConversations(
    ids: List<Conversation.Id>
  ) = withContext(Dispatchers.IO) {
    val idValues = ids.map { it.value }
    firestore.deleteConversations(idValues)
    persistedDB.transaction {
      idValues.forEach {
        persistedDB.chatConversationParticipantQueries.deleteByConversation(it)
        persistedDB.chatConversationQueries.deleteById(it)
      }
    }
  }

  override fun subscribeOnUnreadCounts(): Flow<Unit> {
    return persistedDB.chatConversationQueries
      .selectAllIds()
      .observeList()
      .flatMapLatest { conversationIds ->
        conversationIds
          .map { id -> subscribeOnUnreadCount(id) }
          .merge()
      }
      .flowOn(Dispatchers.IO)
  }

  override fun participant(
    conversationId: Conversation.Id,
    userId: UserId
  ): Flow<Participant?> {
    return persistedDB.chatConversationParticipantQueries
      .selectByConversationAndId(conversationId.value, userId.value)
      .observeOneOrNull()
      .map { selectByConversationAndId ->
        if (selectByConversationAndId == null) {
          return@map null
        }
        Participant(
          id = Participant.Id(selectByConversationAndId.id),
          displayName = selectByConversationAndId.displayName,
          photoUrl = selectByConversationAndId.photoUrl
        )
      }
  }

  override fun participants(
    conversationId: Conversation.Id
  ): Flow<List<Participant>> {
    return persistedDB.chatConversationParticipantQueries
      .selectByConversation(conversationId.value)
      .observeList()
      .map { rows ->
        rows.map { row ->
          Participant(
            id = Participant.Id(row.id),
            displayName = row.displayName,
            photoUrl = row.photoUrl
          )
        }
      }
  }

  private suspend fun applyChange(change: FirestoreChange<ConversationNM>) {
    val conversation = change.data
    when (change.changeType) {
      FirestoreDocumentResult.Added -> {
        addConversation(conversation)
      }
      FirestoreDocumentResult.Modified -> {
        upsertConversation(conversation)
      }
      FirestoreDocumentResult.Removed -> {
        persistedDB.transaction {
          persistedDB.chatConversationParticipantQueries.deleteByConversation(conversation.id)
          persistedDB.chatConversationQueries.deleteById(id = conversation.id)
        }
      }
    }
  }

  private suspend fun addConversation(conversation: ConversationNM) {
    val profiles = conversation.participantUids.mapNotNull { uid ->
      firestore.getUser(UserId(uid))
    }
    persistedDB.transaction {
      conversation.participantUids.forEach { participantId ->
        persistedDB.chatConversationParticipantQueries.insertOrReplace(
          conversationId = conversation.id,
          id = participantId
        )
      }
      profiles.forEach { profile ->
        persistedDB.userQueries.insertOrReplace(
          id = profile.id,
          email = profile.email,
          displayName = profile.displayName,
          photoUrl = profile.photoUrl
        )
      }
      persistedDB.chatConversationQueries.insertOrReplaceMeta(
        id = conversation.id,
        type = conversation.type.value,
        participantUids = conversation.participantUids,
        lastCommit = conversation.lastCommitText,
        lastCommitTimestamp = conversation.lastCommitAt?.toEpochSeconds() ?: 0L
      )
    }
  }

  private fun upsertConversation(conversationNM: ConversationNM) {
    persistedDB.chatConversationQueries.insertOrReplaceMeta(
      id = conversationNM.id,
      type = conversationNM.type.value,
      participantUids = conversationNM.participantUids,
      lastCommit = conversationNM.lastCommitText,
      lastCommitTimestamp = conversationNM.lastCommitAt?.toEpochSeconds() ?: 0L
    )
  }

  private fun subscribeOnUnreadCount(id: String): Flow<Unit> {
    return firestore.unreadCountLive(id).map { unreadCount ->
      persistedDB.chatConversationQueries.updateUnreadCount(
        id = id,
        unreadCount = unreadCount
      )
    }
  }

  private fun userId(): Flow<UserId> {
    return flow {
      val userId = authSessionPersistence.withKey { readUserId(it) }
      emit(requireNotNull(userId))
    }
  }
}
