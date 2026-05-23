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
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.feature.chat.conversation.data.mapper.selectAll
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.conversation.domain.entity.Participant
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import javax.inject.Inject

@SingleIn(ConversationScope::class)
@ContributesBinding(ConversationScope::class)
class ConversationRepositoryImpl @Inject constructor(
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) : ConversationRepository {

  override fun userId(): Flow<UserId> {
    return flow {
      val userId = authSessionPersistence.withKey { readUserId(it) }
      emit(requireNotNull(userId))
    }
  }

  override fun subscribeOnConversations(): Flow<Unit> {
    return firestore.conversationsLive()
      .map { changes -> changes.forEach { applyChange(it) } }
      .flowOn(Dispatchers.IO)
  }

  private suspend fun applyChange(conversation: FirestoreConversation) {
    when (conversation.changeType) {
      null -> Unit
      FirestoreDocumentResult.Added -> {
        addConversation(conversation)
      }
      FirestoreDocumentResult.Modified -> {
        upsertConversation(conversation)
      }
      FirestoreDocumentResult.Removed -> {
        inMemoryDB.transaction {
          inMemoryDB.conversationParticipantQueries.deleteByConversation(conversation.id.value)
          inMemoryDB.chatConversationQueries.deleteById(id = conversation.id.value)
        }
      }
    }
  }

  private suspend fun addConversation(conversation: FirestoreConversation) {
    // Participants подтягиваем ДО локального upsert'а, чтобы SQL никогда не отдал
    // conversation без peer'а в join'е (иначе mapper падает на requireNotNull(peerId)).
    val participants = firestore.getParticipants(conversation.id)
    inMemoryDB.transaction {
      participants.forEach { participant ->
        inMemoryDB.conversationParticipantQueries.insertOrReplace(
          conversationId = conversation.id.value,
          id = participant.id.value,
          displayName = participant.displayName,
          photoUrl = participant.photoUrl
        )
      }
      inMemoryDB.chatConversationQueries.insertOrReplaceMeta(
        id = conversation.id.value,
        type = conversation.type.value,
        participantUids = conversation.participantUids,
        lastCommit = conversation.lastCommitText,
        lastCommitTimestamp = conversation.lastCommitAtEpochSeconds
      )
    }
  }

  private fun upsertConversation(conversation: FirestoreConversation) {
    inMemoryDB.chatConversationQueries.insertOrReplaceMeta(
      id = conversation.id.value,
      type = conversation.type.value,
      participantUids = conversation.participantUids,
      lastCommit = conversation.lastCommitText,
      lastCommitTimestamp = conversation.lastCommitAtEpochSeconds
    )
  }

  override fun subscribeOnUnreadCounts(): Flow<Unit> {
    return inMemoryDB.chatConversationQueries
      .selectAllIds()
      .observeList()
      .flatMapLatest { conversationIds ->
        conversationIds
          .map { id -> subscribeOnUnreadCount(FirestoreConversation.Id(id)) }
          .merge()
      }
  }

  private fun subscribeOnUnreadCount(id: FirestoreConversation.Id): Flow<Unit> {
    return firestore.unreadCountLive(id).map { unreadCount ->
      inMemoryDB.chatConversationQueries.updateUnreadCount(
        id = id.value,
        unreadCount = unreadCount
      )
    }
  }

  override suspend fun deleteConversations(
    ids: List<Conversation.Id>
  ) = withContext(Dispatchers.IO) {
    val idValues = ids.map { it.value }
    firestore.deleteConversations(idValues)
    inMemoryDB.transaction {
      idValues.forEach {
        inMemoryDB.conversationParticipantQueries.deleteByConversation(it)
        inMemoryDB.chatConversationQueries.deleteById(it)
      }
    }
  }

  override val conversations: Flow<List<Conversation>> = userId().flatMapLatest { userId ->
    inMemoryDB.chatConversationQueries
      .selectAll(userId)
      .observeList()
  }

  override fun participant(
    conversationId: Conversation.Id,
    userId: UserId
  ): Flow<Participant?> {
    return inMemoryDB.conversationParticipantQueries
      .selectByConversationAndId(conversationId.value, userId.value)
      .observeOneOrNull()
      .map { row ->
        row?.let {
          Participant(
            id = Participant.Id(it.id),
            displayName = it.displayName,
            photoUrl = it.photoUrl
          )
        }
      }
  }
}
