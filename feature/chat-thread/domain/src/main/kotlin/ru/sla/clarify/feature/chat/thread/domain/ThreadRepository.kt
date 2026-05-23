package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit

interface ThreadRepository {
  fun observeCommitsChanges(branchId: Branch.Id?): Flow<Unit>
  fun commits(branchId: Branch.Id?): Flow<List<Commit>>

  suspend fun fetchHistoryCommits(
    branchId: Branch.Id?,
    count: Int,
    before: Commit? = null
  )

  suspend fun sendCommit(
    branchId: Branch.Id?,
    colorHex: String?,
    text: String
  )

  suspend fun markAsRead()
}
