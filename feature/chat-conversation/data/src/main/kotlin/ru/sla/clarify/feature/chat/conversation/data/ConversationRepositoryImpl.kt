package ru.sla.clarify.feature.chat.conversation.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.feature.chat.conversation.data.mapper.selectAll
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
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
    return firestore.observeConversations()
      .map { changes ->
        inMemoryDB.transaction { changes.forEach { changeConversation(it) } }
      }
  }

  override fun subscribeOnUnreadCounts(): Flow<Unit> {
    return inMemoryDB.chatConversationQueries
      .selectAllIds()
      .asFlow()
      .mapToList(Dispatchers.IO)
      .distinctUntilChanged()
      .flatMapLatest { conversationIds ->
        conversationIds
          .map { id -> subscribeOnUnreadCount(FirestoreConversation.Id(id)) }
          .merge()
      }
  }

  private fun subscribeOnUnreadCount(id: FirestoreConversation.Id): Flow<Unit> {
    return firestore.observeUnreadCount(id).map { unreadCount ->
      inMemoryDB.chatConversationQueries.updateUnreadCount(
        id = id.value,
        unreadCount = unreadCount
      )
    }
  }

  private fun changeConversation(conversation: FirestoreConversation) {
    when (conversation.changeType) {
      null -> Unit
      FirestoreDocumentResult.Added,
      FirestoreDocumentResult.Modified -> {
        inMemoryDB.chatConversationQueries.upsertMeta(
          id = conversation.id.value,
          type = conversation.type.value,
          participantUids = conversation.participantUids,
          lastCommit = conversation.lastCommitText,
          lastCommitTimestamp = conversation.lastCommitAtEpochSeconds
        )
      }

      FirestoreDocumentResult.Removed -> {
        inMemoryDB.chatConversationQueries.delete(
          id = conversation.id.value
        )
      }
    }
  }

  override suspend fun deleteConversations(
    ids: List<Conversation.Id>
  ) = withContext(Dispatchers.IO) {
    val idValues = ids.map { it.value }
    firestore.deleteConversations(idValues)
    inMemoryDB.transaction { idValues.forEach { inMemoryDB.chatConversationQueries.delete(it) } }
  }

  override val conversations: Flow<List<Conversation>> = userId().flatMapLatest { userId ->
    inMemoryDB.chatConversationQueries
      .selectAll(userId)
      .asFlow()
      .mapToList(Dispatchers.IO)
  }
}
