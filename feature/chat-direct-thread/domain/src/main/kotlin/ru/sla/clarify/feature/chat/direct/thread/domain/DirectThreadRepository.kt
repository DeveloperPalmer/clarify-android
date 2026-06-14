package ru.sla.clarify.feature.chat.direct.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.entity.chat.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Member
import ru.sla.clarify.feature.entity.chat.Peer
import java.time.LocalDateTime

interface DirectThreadRepository {
  suspend fun subscribeOnPeerChanges()
  suspend fun subscribeOnCommitChanges()
  suspend fun subscribeOnBranchesChanges()
  suspend fun subscribeOnBranchesUnreadCounts()

  suspend fun fetchHistoryCommits(count: Int, before: Commit? = null)
  suspend fun sendCommit(colorHex: String?, text: String)

  suspend fun markAsRead()
  suspend fun markReadUpTo(lastReadAt: LocalDateTime)

  suspend fun createBranch(parentId: Branch.Id?, from: Commit.Id, name: String): Branch

  val peer: Flow<Peer?>
  val commits: Flow<List<Commit>>
  val unreadCount: Flow<Long>

  val members: Flow<List<Member>>
  fun member(initiator: UserId): Flow<Member?>

  val branches: Flow<List<Branch>>
}
