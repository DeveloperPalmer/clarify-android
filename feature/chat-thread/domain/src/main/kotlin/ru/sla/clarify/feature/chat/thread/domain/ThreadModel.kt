package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.mapDistinctChanges
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.entity.Participant
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import javax.inject.Inject

@SingleIn(ThreadScope::class)
class ThreadModel @Inject constructor(
  private val threadRepository: ThreadRepository,
  private val branchRepository: BranchRepository,
  private val conversationRepository: ConversationRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    threadRepository.observeCommitsChanges(null)
      .launchIn(scope)
    branchRepository.observeBranchChanges()
      .launchIn(scope)
  }

  val user: Flow<User?> = conversationRepository.user

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

  fun branch(id: Branch.Id): Flow<Branch?> {
    return branchRepository.branch(id)
  }

  fun mergeRequestInitiator(id: Branch.Id): Flow<Participant?> {
    return branchRepository.branch(id)
      .mapDistinctChanges { branch ->
        branch?.mergeRequest?.initiatorUid?.let { it to branch.conversationId }
      }
      .flatMapLatest { pair ->
        if (pair == null) {
          flowOf<Participant?>(null)
        } else {
          val (initiatorUid, conversationId) = pair
          conversationRepository.participant(conversationId, initiatorUid)
        }
      }
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
      parentId = parentBranchId,
      branchedFrom = branchedFromCommitId.id,
      name = name
    )
  }

  val openMergeRequest = task<Branch.Id, Unit>(
    name = "openMergeRequest"
  ) { branchId ->
    branchRepository.openMergeRequest(branchId)
  }

  val approveMergeRequest = task<Branch.Id, Unit>(
    name = "approveMergeRequest"
  ) { branchId ->
    branchRepository.approveMergeRequest(branchId)
  }

  val revokeApprovalMergeRequest = task<Branch.Id, Unit>(
    name = "revokeApprovalMergeRequest"
  ) { branchId ->
    branchRepository.revokeApprovalMergeRequest(branchId)
  }

  val cancelMergeRequest = task<Branch.Id, Unit>(
    name = "cancelMergeRequest"
  ) { branchId ->
    branchRepository.cancelMergeRequest(branchId)
  }
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
