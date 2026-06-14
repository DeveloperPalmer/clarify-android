package ru.sla.clarify.feature.chat.branch.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.entity.chat.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Member
import java.time.LocalDateTime

interface BranchRepository {
  suspend fun subscribeOnBranchesChanges()
  suspend fun subscribeOnBranchCommitsChanges()
  suspend fun subscribeOnBranchUnreadCount()

  suspend fun fetchHistoryCommits(count: Int, before: Commit? = null)
  suspend fun sendCommit(colorHex: String?, text: String)

  suspend fun markAsRead()
  suspend fun markReadUpTo(lastReadAt: LocalDateTime)

  suspend fun openMergeRequest()
  suspend fun revokeApprovalMergeRequest()
  suspend fun cancelMergeRequest()
  suspend fun approveMergeRequest()
  suspend fun finalizeMergeRequest()

  val user: Flow<User?>

  val branch: Flow<Branch?>
  val commits: Flow<List<Commit>>
  val unreadCount: Flow<Long>

  val members: Flow<List<Member>>
  fun member(id: UserId): Flow<Member?>
}
