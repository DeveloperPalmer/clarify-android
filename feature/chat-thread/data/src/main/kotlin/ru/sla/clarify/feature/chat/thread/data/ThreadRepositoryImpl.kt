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
import ru.sla.clarify.lib.google.firestore.entity.FirestoreBranch
import ru.sla.clarify.lib.google.firestore.entity.FirestoreCommit
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
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
    val effectiveBranchId = branchId?.value ?: conversationId.value
    val commitsFlow = inMemoryDB.chatCommitQueries
      .selectByBranchId(
        conversationId = conversationId.value,
        branchId = effectiveBranchId,
        mapper = ::mapToCommit
      )
      .observeList()

    emitAll(commitsFlow)
  }

  override fun observeCommitsChanges(branchId: Branch.Id?): Flow<Unit> = flow {
    val conversationId = threadMediator.awaitConversationId()
    val userId = threadMediator.requireUserId()
    val rootBranchId = FirestoreBranch.Id(conversationId.value)
    val resolvedBranchId = branchId?.value?.let(FirestoreBranch::Id) ?: rootBranchId

    firestore.observeDirectCommits(
      peerId = peerId,
      branchId = resolvedBranchId,
      limit = LIVE_COMMIT_LIMIT
    ).collect { changes ->
      applyCommitChanges(
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
    val effectiveBranchId = branchId?.value ?: conversationId.value
    val historyCommits = firestore.historyCommits(
      conversationId = conversationId,
      branchId = FirestoreBranch.Id(effectiveBranchId),
      count = count,
      before = before?.timestamp
    )
    saveHistoryCommits(historyCommits)
  }

  override suspend fun sendCommit(
    branchId: Branch.Id?,
    colorHex: String?,
    text: String
  ) {
    firestore.sendCommit(
      conversationId = threadMediator.conversationId(),
      text = text,
      peerId = peerId,
      branchId = branchId?.value?.let(FirestoreBranch::Id),
      colorHex = colorHex ?: generateColorHex()
    )
  }

  override suspend fun markAsRead() {
    val conversationId = threadMediator.conversationId() ?: return
    firestore.markConversationAsRead(conversationId)
  }

  private suspend fun saveHistoryCommits(
    commits: List<FirestoreCommit>
  ): Unit = withContext(Dispatchers.IO) {
    val userId = threadMediator.requireUserId()
    inMemoryDB.transaction {
      commits.forEach { item ->
        inMemoryDB.chatCommitQueries.insertOrReplace(
          id = item.commitId.value,
          conversationId = item.conversationId.value,
          branchId = item.branchId.value,
          senderId = item.senderId.value,
          text = item.text,
          colorHex = item.colorHex,
          timestamp = item.createdAtEpochSeconds,
          isSelf = item.senderId == userId,
          status = Commit.Status.Sent.value
        )
      }
    }
  }

  private suspend fun applyCommitChanges(
    userId: UserId,
    changes: List<FirestoreCommit>
  ): Unit = withContext(Dispatchers.IO) {
    inMemoryDB.transaction {
      changes.forEach { item ->
        when (item.changeType) {
          FirestoreDocumentResult.Removed -> {
            inMemoryDB.chatCommitQueries.deleteById(
              id = item.commitId.value
            )
          }
          null,
          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            inMemoryDB.chatCommitQueries.insertOrReplace(
              id = item.commitId.value,
              conversationId = item.conversationId.value,
              branchId = item.branchId.value,
              senderId = item.senderId.value,
              text = item.text,
              colorHex = item.colorHex,
              timestamp = item.createdAtEpochSeconds,
              isSelf = item.senderId == userId,
              status = Commit.Status.Sent.value
            )
          }
        }
      }
    }
  }
}

private const val LIVE_COMMIT_LIMIT = 50L
