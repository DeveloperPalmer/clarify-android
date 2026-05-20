package ru.sla.clarify.feature.chat.conversation.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.feature.chat.conversation.data.mapper.ConversationMappers
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.log.log
import javax.inject.Inject

@SingleIn(ConversationScope::class)
@ContributesBinding(ConversationScope::class)
class ConversationRepositoryImpl @Inject constructor(
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) : ConversationRepository {

  override fun userId(): Flow<UserId?> {
    return flow { emit(authSessionPersistence.withKey { readUserId(it) }) }
  }

  override fun subscribeOnConversations(): Flow<Unit> {
    return firestore.observeConversations()
      .map { changes ->
        val userId = requireNotNull(userId().firstOrNull())
        changes.forEach { conversation ->
          when (conversation.changeType) {
            null -> Unit
            FirestoreDocumentResult.Added,
            FirestoreDocumentResult.Modified -> {
              saveConversation(conversation, userId)
            }
            FirestoreDocumentResult.Removed -> {
              deleteConversationById(conversation.id)
            }
          }
        }
      }
  }

  override suspend fun deleteConversations(ids: List<Conversation.Id>) {
    firestore.deleteConversations(ids.map { it.value })
    ids.forEach { id -> deleteConversationById(id.value) }
  }

  override val conversations: Flow<List<Conversation>> = inMemoryDB.chatConversationQueries
    .selectAll(ConversationMappers::mapToConversation)
    .asFlow()
    .mapToList(Dispatchers.IO)

  private fun deleteConversationById(id: String) {
    inMemoryDB.chatConversationQueries.delete(id)
  }

  private fun saveConversation(
    item: FirestoreConversation,
    userId: UserId
  ) {
    val peerId = item.participantUids.firstOrNull { it != userId.value }
    if (peerId == null) {
      log { "Chat: skip conversation ${item.id} without peer uid" }
      return
    }
    inMemoryDB.chatConversationQueries.insertOrReplace(
      id = item.id,
      peerId = peerId,
      unreadCount = 0L,
      lastCommit = item.lastCommitText,
      lastCommitTimestamp = item.lastCommitAtEpochSeconds
    )
  }
}
