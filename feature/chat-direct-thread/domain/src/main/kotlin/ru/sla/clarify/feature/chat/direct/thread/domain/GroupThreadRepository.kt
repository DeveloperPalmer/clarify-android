package ru.sla.clarify.feature.chat.direct.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.entity.chat.Commit
import java.time.LocalDateTime

interface GroupThreadRepository {
  suspend fun subscribeOnCommitChanges()

  suspend fun fetchHistoryCommits(count: Int, before: Commit? = null)
  suspend fun sendCommit(text: String)

  suspend fun markReadUpTo(lastReadAt: LocalDateTime)

  val commits: Flow<List<Commit>>
  val unreadCount: Flow<Long>
}
