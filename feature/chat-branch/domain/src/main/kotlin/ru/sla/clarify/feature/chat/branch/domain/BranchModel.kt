package ru.sla.clarify.feature.chat.branch.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.mapDistinctNotNullChanges
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(BranchScope::class)
class BranchModel @Inject constructor(
  private val branchRepository: BranchRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    scope.launch { branchRepository.subscribeOnBranchesChanges() }
    scope.launch { branchRepository.subscribeOnBranchCommitsChanges() }
    scope.launch { branchRepository.subscribeOnBranchUnreadCount() }
  }

  fun markReadCommits() {
    scope.launch { branchRepository.markAsRead() }
  }

  fun markReadUpTo(lastReadAt: LocalDateTime) {
    scope.launch { branchRepository.markReadUpTo(lastReadAt) }
  }

  val fetchHistoryCommits = task<Unit>(
    name = "fetchHistoryCommits"
  ) {
    branchRepository.fetchHistoryCommits(
      count = DEFAULT_HISTORY_PAGE_SIZE
    )
  }

  val sendMessage = task<String?, Unit>(
    name = "sendMessage"
  ) { text ->
    branchRepository.sendCommit(text = requireNotNull(text))
  }

  val openMergeRequest = task<Unit>(
    name = "openMergeRequest"
  ) {
    branchRepository.openMergeRequest()
  }

  val approveMergeRequest = task<Unit>(
    name = "approveMergeRequest"
  ) {
    branchRepository.approveMergeRequest()
  }

  val revokeApprovalMergeRequest = task<Unit>(
    name = "revokeApprovalMergeRequest"
  ) {
    branchRepository.revokeApprovalMergeRequest()
  }

  val cancelMergeRequest = task<Unit>(
    name = "cancelMergeRequest"
  ) {
    branchRepository.cancelMergeRequest()
  }

  val finalizeMergeRequest = task<Unit>(
    name = "finalizeMergeRequest"
  ) {
    branchRepository.finalizeMergeRequest()
  }

  val user: Flow<User?> = branchRepository.user

  val commits: Flow<List<Commit>> = branchRepository.commits
  val unreadCount: Flow<Long> = branchRepository.unreadCount

  val members: Flow<List<Member>> = branchRepository.members

  val branch: Flow<Branch?> = branchRepository.branch

  val mergeRequestInitiator: Flow<Member?> = branchRepository.branch
    .mapDistinctNotNullChanges { it?.mergeRequest?.initiatorId }
    .flatMapLatest(branchRepository::member)
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
