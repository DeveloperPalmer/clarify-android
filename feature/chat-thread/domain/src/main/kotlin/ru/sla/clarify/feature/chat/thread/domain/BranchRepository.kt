package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit

interface BranchRepository {
  fun observeBranchChanges(): Flow<Unit>

  fun branches(): Flow<List<Branch>>
  fun branch(id: Branch.Id): Flow<Branch?>

  suspend fun createBranch(
    parentBranchId: Branch.Id?,
    branchedFromCommitId: Commit.Id,
    name: String
  ): Branch

  /**
   * Open a merge request for [branchId]. Branch transitions to [Branch.Status.MergeInProgress].
   * The initiator counts as the first approver.
   */
  suspend fun requestMerge(branchId: Branch.Id)

  /**
   * Add the current user to the merge approver list. If every conversation participant has
   * approved, the branch is finalized (status -> [Branch.Status.Merged]) atomically.
   */
  suspend fun approveMerge(branchId: Branch.Id)

  /**
   * Remove the current user from the approver list. If the current user is the initiator,
   * this is equivalent to cancelling the merge request.
   */
  suspend fun revokeApproval(branchId: Branch.Id)

  /**
   * Initiator-only: cancel the merge request. Branch returns to [Branch.Status.Active].
   */
  suspend fun cancelMergeRequest(branchId: Branch.Id)
}
