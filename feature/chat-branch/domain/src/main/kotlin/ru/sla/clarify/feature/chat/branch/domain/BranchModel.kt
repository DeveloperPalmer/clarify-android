package ru.sla.clarify.feature.chat.branch.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.mapDistinctNotNullChanges
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import ru.sla.clarify.feature.chat.branch.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Participant
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(BranchScope::class)
class BranchModel @Inject constructor(
  private val branchRepository: BranchRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    scope.launch { branchRepository.subscribeOnChanges() }
    scope.launch { branchRepository.subscribeOnCommitChanges() }
    scope.launch { branchRepository.subscribeOnUnreadCount() }
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

  val sendMessage = task<String, String?, Unit>(
    name = "sendMessage"
  ) { text, colorHex ->
    branchRepository.sendCommit(
      text = text,
      colorHex = colorHex
    )
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

  val participants: Flow<List<Participant>> = branchRepository.participants

  val branch: Flow<Branch?> = branchRepository.branch

  val mergeRequestInitiator: Flow<Participant?> = branchRepository.branch
    .mapDistinctNotNullChanges { it?.mergeRequest?.initiatorId }
    .flatMapLatest(branchRepository::participant)
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
