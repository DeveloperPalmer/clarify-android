package ru.sla.clarify.feature.chat.direct.thread.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.feature.chat.direct.thread.data.common.ThreadMediator
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.generateColorHex
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.mapToCommit
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.mapToParticipant
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.mapToPeer
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.toLocalDateTime
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.withReadStatus
import ru.sla.clarify.feature.chat.direct.thread.domain.ThreadRepository
import ru.sla.clarify.feature.chat.direct.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Participant
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.entity.UserNM
import ru.sla.clarify.lib.google.firestore.toEpochSeconds
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(ThreadScope::class)
@ContributesBinding(ThreadScope::class)
class ThreadRepositoryImpl @Inject constructor(
  private val peerId: Peer.Id,
  private val firestore: Firestore,
  private val persistedDB: PersistedDB,
  private val threadMediator: ThreadMediator
) : ThreadRepository {

  private var lastReadWatermark: LocalDateTime? = null

  override suspend fun subscribeOnPeerChanges() {
    val peerId = UserId(peerId.value)
    firestore.userLive(peerId)
      .filterNotNull()
      .flowOn(Dispatchers.IO)
      .collect(::applyPeerChanges)
  }

  override suspend fun subscribeOnCommitChanges() {
    val userId = threadMediator.requireUserId()
    val conversationId = threadMediator.awaitConversationId()
    firestore.directCommitsLive(
      peerId = peerId,
      branchId = conversationId,
      limit = LIVE_COMMIT_LIMIT
    ).flowOn(
      context = Dispatchers.IO
    ).collect { changes ->
      applyCommitChanges(
        conversationId = conversationId,
        userId = userId,
        changes = changes
      )
    }
  }

  override suspend fun fetchHistoryCommits(
    count: Int,
    before: Commit?
  ) {
    val conversationId = threadMediator.awaitConversationId()
    val historyCommits = firestore.getCommits(
      conversationId = conversationId,
      branchId = conversationId,
      count = count,
      before = before?.timestamp
    )
    applyInsertOrReplaceCommits(
      commits = historyCommits
    )
  }

  override suspend fun sendCommit(
    colorHex: String?,
    text: String
  ) {
    firestore.postCommit(
      conversationId = threadMediator.conversationId(),
      text = text,
      peerId = peerId,
      branchId = null,
      colorHex = colorHex ?: generateColorHex()
    )
  }

  override suspend fun markAsRead() {
    val conversationId = threadMediator.conversationId() ?: return
    firestore.patchClearUnreadCount(conversationId)
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    val conversationId = threadMediator.conversationId() ?: return
    val current = lastReadWatermark
    if (current != null && !lastReadAt.isAfter(current)) return
    lastReadWatermark = lastReadAt
    firestore.patchReadWatermark(conversationId, lastReadAt)
    firestore.patchClearUnreadCount(conversationId)
  }

  override val peer: Flow<Peer?> = persistedDB.userQueries
    .selectById(peerId.value, ::mapToPeer)
    .observeOneOrNull()

  override fun commits(): Flow<List<Commit>> = flow {
    val peerId = UserId(peerId.value)
    val conversationId = threadMediator.awaitConversationId()

    val commitsFlow = persistedDB.chatCommitQueries
      .selectByBranchId(conversationId, conversationId, ::mapToCommit)
      .observeList()

    val peerReadAtFlow = firestore
      .participantLive(conversationId, peerId)
      .map { it?.lastReadAt?.toLocalDateTime() }

    val result = combine(
      flow = commitsFlow,
      flow2 = peerReadAtFlow
    ) { commits, peerReadAt ->
      commits.map { it.withReadStatus(peerReadAt) }
    }

    emitAll(result)
  }

  override val participants: Flow<List<Participant>> = flow {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.chatConversationParticipantQueries
      .selectByConversation(conversationId, ::mapToParticipant)
      .observeList()
      .collect { emit(it) }
  }

  override fun participant(initiator: UserId): Flow<Participant?> = flow {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.chatConversationParticipantQueries
      .selectByConversationAndId(conversationId, initiator.value, ::mapToParticipant)
      .observeOneOrNull()
      .collect { emit(it) }
  }

  override val unreadCount: Flow<Long> = flow {
    val conversationId = threadMediator.awaitConversationId()
    firestore.unreadCountLive(conversationId)
      .collect { emit(it) }
  }

  private suspend fun applyInsertOrReplaceCommits(commits: List<CommitNM>) {
    return withContext(Dispatchers.IO) {
      val userId = threadMediator.requireUserId()
      val conversationId = threadMediator.awaitConversationId()
      persistedDB.transaction {
        commits.forEach { item ->
          applyInsertOrReplaceCommit(
            conversationId = conversationId,
            commit = item,
            userId = userId,
            hasPendingWrites = false
          )
        }
      }
    }
  }

  private suspend fun applyPeerChanges(user: UserNM): Unit = withContext(Dispatchers.IO) {
    persistedDB.userQueries.insertOrReplace(
      id = user.id,
      email = user.email,
      displayName = user.displayName,
      photoUrl = user.photoUrl
    )
  }

  private suspend fun applyCommitChanges(
    conversationId: String,
    userId: UserId,
    changes: List<FirestoreChange<CommitNM>>
  ): Unit = withContext(Dispatchers.IO) {
    persistedDB.transaction {
      changes.forEach { change ->
        val commit = change.data
        when (change.changeType) {
          FirestoreDocumentResult.Removed -> {
            persistedDB.chatCommitQueries.deleteById(commit.id)
          }
          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            applyInsertOrReplaceCommit(
              conversationId = conversationId,
              commit = commit,
              userId = userId,
              hasPendingWrites = change.hasPendingWrites
            )
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceCommit(
    conversationId: String,
    commit: CommitNM,
    userId: UserId,
    hasPendingWrites: Boolean
  ) {
    persistedDB.chatCommitQueries.insertOrReplace(
      id = commit.id,
      conversationId = conversationId,
      branchId = commit.branchId,
      senderId = commit.senderUid,
      type = commit.type.value,
      text = commit.text.orEmpty(),
      invitedUid = commit.invitedUid,
      colorHex = commit.colorHex.orEmpty(),
      timestamp = commit.createdAt?.toEpochSeconds() ?: 0L,
      isSelf = commit.senderUid == userId.value,
      status = if (hasPendingWrites) {
        Commit.Status.Sending.value
      } else {
        Commit.Status.Sent.value
      }
    )
  }
}

private const val LIVE_COMMIT_LIMIT = 50L
