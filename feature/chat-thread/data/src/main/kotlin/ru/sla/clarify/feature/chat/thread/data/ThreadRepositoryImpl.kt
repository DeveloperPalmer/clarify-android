package ru.sla.clarify.feature.chat.thread.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.InMemoryDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.feature.chat.thread.data.common.ThreadMediator
import ru.sla.clarify.feature.chat.thread.data.mapper.generateColorHex
import ru.sla.clarify.feature.chat.thread.data.mapper.mapToCommit
import ru.sla.clarify.feature.chat.thread.domain.ThreadRepository
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.toEpochSeconds
import javax.inject.Inject

@SingleIn(ThreadScope::class)
@ContributesBinding(ThreadScope::class)
class ThreadRepositoryImpl @Inject constructor(
  private val peerId: Peer.Id,
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val threadMediator: ThreadMediator
) : ThreadRepository {

  override fun commits(branchId: Branch.Id?): Flow<List<Commit>> = flow {
    val conversationId = threadMediator.awaitConversationId()
    val effectiveBranchId = branchId?.value ?: conversationId
    val commitsFlow = inMemoryDB.chatCommitQueries
      .selectByBranchId(
        conversationId = conversationId,
        branchId = effectiveBranchId,
        mapper = ::mapToCommit
      )
      .observeList()

    emitAll(commitsFlow)
  }

  override fun observeCommitsChanges(branchId: Branch.Id?): Flow<Unit> = flow {
    val conversationId = threadMediator.awaitConversationId()
    val userId = threadMediator.requireUserId()
    val resolvedBranchId = branchId?.value ?: conversationId

    firestore.directCommitsLive(
      peerId = peerId,
      branchId = resolvedBranchId,
      limit = LIVE_COMMIT_LIMIT
    ).collect { changes ->
      applyCommitChanges(
        conversationId = conversationId,
        userId = userId,
        changes = changes
      )
      emit(Unit)
    }
  }

  override suspend fun fetchHistoryCommits(
    branchId: Branch.Id?,
    count: Int,
    before: Commit?
  ) {
    val conversationId = threadMediator.conversationId() ?: return
    val effectiveBranchId = branchId?.value ?: conversationId
    val historyCommits = firestore.getCommits(
      conversationId = conversationId,
      branchId = effectiveBranchId,
      count = count,
      before = before?.timestamp
    )
    saveHistoryCommits(conversationId, historyCommits)
  }

  override suspend fun sendCommit(
    branchId: Branch.Id?,
    colorHex: String?,
    text: String
  ) {
    firestore.postCommit(
      conversationId = threadMediator.conversationId(),
      text = text,
      peerId = peerId,
      branchId = branchId?.value,
      colorHex = colorHex ?: generateColorHex()
    )
  }

  override suspend fun markAsRead() {
    val conversationId = threadMediator.conversationId() ?: return
    firestore.patchUnreadCount(conversationId)
  }

  private suspend fun saveHistoryCommits(
    conversationId: String,
    commitNMS: List<CommitNM>
  ): Unit = withContext(Dispatchers.IO) {
    val userId = threadMediator.requireUserId()
    inMemoryDB.transaction {
      commitNMS.forEach { item -> insertOrReplaceCommit(conversationId, item, userId) }
    }
  }

  private suspend fun applyCommitChanges(
    conversationId: String,
    userId: UserId,
    changes: List<FirestoreChange<CommitNM>>
  ): Unit = withContext(Dispatchers.IO) {
    inMemoryDB.transaction {
      changes.forEach { change ->
        val commit = change.data
        when (change.changeType) {
          FirestoreDocumentResult.Removed -> {
            inMemoryDB.chatCommitQueries.deleteById(id = commit.id)
          }
          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            insertOrReplaceCommit(conversationId, commit, userId)
          }
        }
      }
    }
  }

  private fun insertOrReplaceCommit(
    conversationId: String,
    commitNM: CommitNM,
    currentUserId: UserId
  ) {
    inMemoryDB.chatCommitQueries.insertOrReplace(
      id = commitNM.id,
      conversationId = conversationId,
      branchId = commitNM.branchId,
      senderId = commitNM.senderUid,
      text = commitNM.text.orEmpty(),
      colorHex = commitNM.colorHex,
      timestamp = commitNM.createdAt?.toEpochSeconds() ?: 0L,
      isSelf = commitNM.senderUid == currentUserId.value,
      status = Commit.Status.Sent.value
    )
  }
}

private const val LIVE_COMMIT_LIMIT = 50L
