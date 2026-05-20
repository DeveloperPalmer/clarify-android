package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Commit

interface ThreadRepository {
  suspend fun conversation(): Conversation?

  fun subscribeOnCommits(): Flow<Unit>
  val commits: Flow<Commit>

  suspend fun fetchHistoryCommits(
    count: Int,
    before: Commit? = null
  )

  suspend fun sendCommit(
    text: String,
    parent: Commit?
  )

  suspend fun markAsRead()
}
