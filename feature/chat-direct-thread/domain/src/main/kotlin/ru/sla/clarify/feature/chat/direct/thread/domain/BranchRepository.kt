package ru.sla.clarify.feature.chat.direct.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit

interface BranchRepository {
  suspend fun subscribeOnBranchChanges()
  suspend fun subscribeOnBranchesUnreadCounts()

  suspend fun createBranch(parentId: Branch.Id?, from: Commit.Id, name: String): Branch

  val branches: Flow<List<Branch>>
}
