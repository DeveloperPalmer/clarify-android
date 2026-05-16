package ru.sla.clarify.feature.chat.thread.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.auth.session.domain.entity.UserId
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.database.InMemoryDB
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
          val commit = saveCommit(message)
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
      saveCommit(
        item = item
      )
    }
  }

  override suspend fun sendMessage(
    text: String,
    parentMessage: Commit?
  ): Commit {
    val item = firestore.sendCommit(
      peerId = peerId,
      text = text,
      parentCommitId = parentMessage?.id?.value,
      colorHex = parentMessage?.colorHex ?: generateColorHex()
    )
    return saveCommit(
      item = item,
      status = Commit.Status.Sent
    )
  }

  override suspend fun markAsRead() {
    firestore.markConversationAsRead(peerId)
  }

  private suspend fun saveCommit(
    item: FirestoreCommit,
    status: Commit.Status = Commit.Status.Sent
  ): Commit = withContext(Dispatchers.IO) {
    val userId = requireUserId()
    val parentId = item.parentCommitId
    val resolvedPeerId = if (item.senderId == userId) {
      peerId.value
    } else {
      item.senderId.value
    }
    val conversationId = firestore.conversationId(peerId)

    inMemoryDB.transaction {
      inMemoryDB.peerQueries.insertIfAbsent(
        id = resolvedPeerId,
        name = null,
        faceUrl = null
      )
      inMemoryDB.chatConversationQueries.insertIfAbsent(
        id = conversationId,
        peerId = resolvedPeerId,
        lastCommit = null,
        lastCommitTimestamp = 0L,
        unreadCount = 0L
      )
      inMemoryDB.chatCommitQueries.insertOrReplace(
        id = Commit.Id(item.commitId.value),
        parentId = parentId?.value?.let(Commit::Id),
        conversationId = conversationId,
        peerId = resolvedPeerId,
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
      parentId = parentId?.value?.let(Commit::Id),
      peerId = resolvedPeerId,
      senderId = item.senderId.value,
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
}

private const val LIVE_MESSAGES_LIMIT = 50L
