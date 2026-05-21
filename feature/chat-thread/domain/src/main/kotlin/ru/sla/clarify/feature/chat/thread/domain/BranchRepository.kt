package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit

interface BranchRepository {
  fun observeBranchChanges(): Flow<Unit>

  fun branches(): Flow<List<Branch>>
  fun branch(id: Branch.Id): Flow<Branch?>

  suspend fun createBranch(
    parentBranchId: Branch.Id,
    branchedFromCommitId: Commit.Id,
    name: String
  ): Branch
}
