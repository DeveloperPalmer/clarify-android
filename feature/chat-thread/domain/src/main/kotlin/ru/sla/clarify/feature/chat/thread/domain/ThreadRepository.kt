package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Peer
import java.time.LocalDateTime

interface ThreadRepository {

  val peer: Flow<Peer?>
  suspend fun subscribeOnPeerChanges()

  fun commits(branchId: Branch.Id?): Flow<List<Commit>>
  suspend fun subscribeOnCommitChanges(branchId: Branch.Id?)

  fun unreadCount(): Flow<Long>

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

  suspend fun markReadUpTo(lastReadAt: LocalDateTime)
}
