package ru.sla.clarify.feature.chat.thread.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.thread.data.mapper.generateColorHex
import ru.sla.clarify.feature.chat.thread.domain.ThreadRepository
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.entity.FirestoreCommit
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@SingleIn(ThreadScope::class)
@ContributesBinding(ThreadScope::class)
class ThreadRepositoryImpl @Inject constructor(
  private val peerId: Peer.Id,
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val authSessionPersistence: AuthSessionPersistence
) : ThreadRepository {

  override suspend fun getConversation(): Conversation? = withContext(Dispatchers.IO) {
    val conversation = firestore.directConversation(peerId)
      ?: return@withContext null
    inMemoryDB.chatConversationQueries
      .selectById(conversation.id, ::mapToConversation)
      .executeAsOneOrNull()
  }

  override fun peerCommits(): Flow<Commit> {
    return channelFlow {
      firestore.observeMessages(
        peerId = peerId,
        limit = LIVE_MESSAGES_LIMIT
      ).collect { changes ->
        changes.forEach { message ->
          if (message.changeType == FirestoreDocumentResult.Removed) {
            return@forEach
          }
          val commit = saveMessage(message)
          if (!commit.isSelf) {
            trySend(commit)
          }
        }
      }
    }
  }

  override suspend fun getCommitHistory(
    count: Int,
    before: Commit?
  ): List<Commit> {
    return firestore.loadMessageHistory(
      peerId = peerId,
      count = count,
      before = before?.timestamp
    ).map { item ->
      saveMessage(
        item = item
      )
    }
  }

  override suspend fun sendMessage(
    text: String,
    parentMessage: Commit.Message?
  ): Commit.Message {
    val item = firestore.sendCommit(
      peerId = peerId,
      text = text,
      colorHex = parentMessage?.colorHex ?: generateColorHex()
    )
    return saveMessage(
      item = item,
      status = Commit.Status.Sent
    )
  }

  override suspend fun markAsRead() {
    firestore.markConversationAsRead(peerId)
  }

  private suspend fun saveMessage(
    item: FirestoreCommit,
    status: Commit.Status = Commit.Status.Sent
  ): Commit.Message = withContext(Dispatchers.IO) {
    val userId = requireUserId()
    val conversation = requireNotNull(firestore.directConversation(peerId)) {
      "Direct conversation not found for peer ${peerId.value}"
    }

    inMemoryDB.transaction {
      inMemoryDB.chatConversationQueries.insertIfAbsent(
        id = conversation.id,
        peerId = peerId.value,
        lastCommit = null,
        lastCommitTimestamp = 0L,
        unreadCount = 0L
      )
      inMemoryDB.chatCommitQueries.insertOrReplace(
        id = item.commitId.value,
        conversationId = conversation.id,
        senderId = item.senderId.value,
        text = item.text,
        colorHex = item.colorHex,
        timestamp = item.createdAtEpochSeconds,
        isSelf = item.senderId == userId,
        status = status.name
      )
    }
    Commit.Message(
      id = Commit.Id(item.commitId.value),
      senderId = item.senderId,
      text = item.text,
      colorHex = item.colorHex,
      timestamp = Instant
        .ofEpochSecond(item.createdAtEpochSeconds)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime(),
      isSelf = item.senderId == userId,
      status = status
    )
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) })
  }

  private fun mapToConversation(
    id: String,
    peerId: String,
    lastMessage: String?,
    lastMessageTimestamp: Long,
    unreadCount: Long
  ): Conversation {
    return Conversation(
      id = Conversation.Id(id),
      peer = Peer(
        id = Peer.Id(peerId),
        faceUrl = null
      ),
      lastMessage = lastMessage,
      lastMessageTimestamp = lastMessageTimestamp,
      unreadCount = unreadCount
    )
  }
}

private const val LIVE_MESSAGES_LIMIT = 50L
