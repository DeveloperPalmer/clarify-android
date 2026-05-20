package ru.sla.clarify.feature.chat.thread.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.flattenItems
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.thread.data.mapper.Mappers
import ru.sla.clarify.feature.chat.thread.data.mapper.generateColorHex
import ru.sla.clarify.feature.chat.thread.domain.ThreadRepository
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.entity.FirestoreCommit
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import javax.inject.Inject

@SingleIn(ThreadScope::class)
@ContributesBinding(ThreadScope::class)
class ThreadRepositoryImpl @Inject constructor(
  private val peerId: Peer.Id,
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) : ThreadRepository {

  override val commits: Flow<Commit> = inMemoryDB.threadQueries
    .selectConversationId(peerId.value)
    .asFlow()
    .mapToOneOrNull(Dispatchers.IO)
    .filterNotNull()
    .flatMapLatest { conversationId ->
      inMemoryDB.chatCommitQueries
        .selectByConversationId(conversationId, Mappers::mapToCommit)
        .asFlow()
        .mapToList(Dispatchers.IO)
        .flattenItems()
    }

  override suspend fun conversation(): Conversation? {
    val conversationId = conversationId() ?: return null
    return withContext(Dispatchers.IO) {
      inMemoryDB.chatConversationQueries
        .selectById(conversationId.value, Mappers::mapToConversation)
        .executeAsOneOrNull()
    }
  }

  override fun subscribeOnCommits(): Flow<Unit> {
    return firestore.observeDirectCommits(
      peerId = peerId,
      limit = LIVE_COMMIT_LIMIT
    )
      .flattenItems()
      .map { commit ->
        if (commit.changeType != FirestoreDocumentResult.Removed) {
          saveCommit(commit)
        }
      }
  }

  override suspend fun fetchHistoryCommits(count: Int, before: Commit?) {
    val conversationId = conversationId() ?: return
    val historyCommits = firestore.historyCommits(
      conversationId = conversationId,
      count = count,
      before = before?.timestamp
    )
    saveHistoryCommits(
      conversationId = conversationId,
      commits = historyCommits
    )
  }

  override suspend fun sendCommit(text: String, parent: Commit?) {
    firestore.sendCommit(
      text = text,
      peerId = peerId,
      colorHex = parent?.colorHex ?: generateColorHex()
    )
  }

  override suspend fun markAsRead() {
    val conversationId = conversationId() ?: return
    firestore.markConversationAsRead(conversationId)
  }

  private suspend fun saveHistoryCommits(
    conversationId: FirestoreConversation.Id,
    commits: List<FirestoreCommit>
  ): Unit = withContext(Dispatchers.IO) {
    val userId = requireUserId()
    inMemoryDB.transaction {
      inMemoryDB.threadQueries.insertOrReplace(
        peerId = peerId.value,
        conversationId = conversationId.value
      )
      commits.forEach { item ->
        inMemoryDB.chatCommitQueries.insertOrReplace(
          id = item.commitId.value,
          conversationId = item.conversationId.value,
          senderId = item.senderId.value,
          text = item.text,
          colorHex = item.colorHex,
          timestamp = item.createdAtEpochSeconds,
          isSelf = item.senderId == userId,
          status = Commit.Status.Sent.name
        )
      }
    }
  }

  private suspend fun saveCommit(
    item: FirestoreCommit,
    status: Commit.Status = Commit.Status.Sent
  ): Unit = withContext(Dispatchers.IO) {
    val userId = requireUserId()
    inMemoryDB.transaction {
      inMemoryDB.threadQueries.insertOrReplace(
        peerId = peerId.value,
        conversationId = item.conversationId.value
      )
      inMemoryDB.chatCommitQueries.insertOrReplace(
        id = item.commitId.value,
        conversationId = item.conversationId.value,
        senderId = item.senderId.value,
        text = item.text,
        colorHex = item.colorHex,
        timestamp = item.createdAtEpochSeconds,
        isSelf = item.senderId == userId,
        status = status.name
      )
    }
  }

  private suspend fun conversationId(): FirestoreConversation.Id? = withContext(Dispatchers.IO) {
    val cachedId = inMemoryDB.threadQueries
      .selectConversationId(peerId.value)
      .executeAsOneOrNull()

    if (cachedId != null) {
      return@withContext FirestoreConversation.Id(cachedId)
    }

    val remoteConversation = firestore.directConversation(peerId)
      ?: return@withContext null

    inMemoryDB.threadQueries.insertOrReplace(
      peerId = peerId.value,
      conversationId = remoteConversation.id.value
    )

    return@withContext remoteConversation.id
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) })
  }
}

private const val LIVE_COMMIT_LIMIT = 50L
