package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import ru.sla.clarify.auth.session.domain.AuthSessionRepository
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import javax.inject.Inject

@SingleIn(ThreadScope::class)
class ThreadModel @Inject constructor(
  private val threadRepository: ThreadRepository,
  private val branchRepository: BranchRepository,
  private val authSessionRepository: AuthSessionRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    // Uncaught exceptions from these subscriptions (e.g. Firestore FAILED_PRECONDITION on a
    // missing composite index) are logged centrally by ReactiveModel via its uncaughtExceptions
    // handler, and SupervisorJob in scope prevents one failing subscription from cancelling
    // sibling tasks like sendMessage.
    threadRepository.observeCommitsChanges(null).launchIn(scope)
    branchRepository.observeBranchChanges().launchIn(scope)
  }

  /**
   * One-shot stream of the current user's id, used by UI to determine "am I the initiator?"
   * of a merge request.
   */
  fun userId(): Flow<UserId> = flow {
    val userId = authSessionRepository.withKey { readUserId(it) }
    emit(requireNotNull(userId))
  }

  fun subscribeOnCommits(branchId: Branch.Id): Flow<Unit> {
    return threadRepository.observeCommitsChanges(branchId)
  }

  fun commits(branchId: Branch.Id?): Flow<List<Commit>> {
    return threadRepository.commits(branchId)
  }

  fun markReadCommits() {
    scope.launch { threadRepository.markAsRead() }
  }

  fun branches(): Flow<List<Branch>> {
    return branchRepository.branches()
  }

  val fetchHistoryCommits = task<Unit>(
    name = "fetchHistoryCommits"
  ) {
    threadRepository.fetchHistoryCommits(
      branchId = null,
      count = DEFAULT_HISTORY_PAGE_SIZE
    )
  }

  val fetchHistoryBranchCommits = task<Branch.Id, Unit>(
    name = "fetchHistoryBranchCommits"
  ) { branchId ->
    threadRepository.fetchHistoryCommits(
      branchId = branchId,
      count = DEFAULT_HISTORY_PAGE_SIZE
    )
  }

  val sendMessage = task<Branch.Id?, String, String?, Unit>(
    name = "sendMessage"
  ) { branchId, text, colorHex ->
    threadRepository.sendCommit(
      branchId = branchId,
      text = text,
      colorHex = colorHex
    )
  }

  val createBranch = task<Branch.Id?, Commit.Message, String, Branch>(
    name = "createBranch"
  ) { parentBranchId, branchedFromCommitId, name ->
    branchRepository.createBranch(
      parentBranchId = parentBranchId,
      branchedFromCommitId = branchedFromCommitId.id,
      name = name
    )
  }

  val requestMerge = task<Branch.Id, Unit>(
    name = "requestMerge"
  ) { branchId ->
    branchRepository.requestMerge(branchId)
  }

  val approveMerge = task<Branch.Id, Unit>(
    name = "approveMerge"
  ) { branchId ->
    branchRepository.approveMerge(branchId)
  }

  val revokeApproval = task<Branch.Id, Unit>(
    name = "revokeApproval"
  ) { branchId ->
    branchRepository.revokeApproval(branchId)
  }

  val cancelMergeRequest = task<Branch.Id, Unit>(
    name = "cancelMergeRequest"
  ) { branchId ->
    branchRepository.cancelMergeRequest(branchId)
  }
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
