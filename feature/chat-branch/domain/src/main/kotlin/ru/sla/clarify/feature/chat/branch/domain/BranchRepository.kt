package ru.sla.clarify.feature.chat.branch.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Member
import java.time.LocalDateTime

interface BranchRepository {
  suspend fun subscribeOnBranchChanges()
  suspend fun subscribeOnBranchCommitsChanges()
  suspend fun subscribeOnBranchUnreadCountChanges()

  suspend fun fetchLatestCommits()
  suspend fun fetchCommitHistory()
  suspend fun sendCommit(text: String, replyCommit: Commit.Message?)
  suspend fun editCommit(id: Commit.Id, text: String)
  suspend fun deleteCommits(ids: List<Commit.Id>, forEveryone: Boolean)

  suspend fun markAsRead()
  suspend fun markReadUpTo(lastReadAt: LocalDateTime)

  suspend fun openMergeRequest()
  suspend fun revokeMergeRequestApproval()
  suspend fun cancelMergeRequest()
  suspend fun approveMergeRequest()
  suspend fun finalizeMergeRequest()

  val user: Flow<User?>

  val branch: Flow<Branch?>
  val commits: Flow<List<Commit>>
  val unreadCount: Flow<Long>

  val hasCommitsHistory: Flow<Boolean>

  val members: Flow<List<Member>>
  fun member(id: UserId): Flow<Member?>
}
