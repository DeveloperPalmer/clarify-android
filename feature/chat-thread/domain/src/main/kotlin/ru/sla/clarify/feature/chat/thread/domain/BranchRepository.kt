package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit

interface BranchRepository {

  fun observeBranchChanges(): Flow<Unit>

  fun branches(): Flow<List<Branch>>

  fun branch(id: Branch.Id): Flow<Branch?>

  suspend fun createBranch(
    parentId: Branch.Id?,
    branchedFrom: Commit.Id,
    name: String
  ): Branch

  suspend fun openMergeRequest(branchId: Branch.Id)

  suspend fun approveMergeRequest(branchId: Branch.Id)

  suspend fun revokeApprovalMergeRequest(branchId: Branch.Id)

  suspend fun cancelMergeRequest(branchId: Branch.Id)

  suspend fun finalizeMergeRequest(branchId: Branch.Id)
}
