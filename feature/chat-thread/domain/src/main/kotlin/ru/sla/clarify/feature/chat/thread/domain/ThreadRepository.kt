package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.entity.chat.Commit

interface ThreadRepository {
  fun peerCommits(): Flow<Commit>

  suspend fun getCommitHistory(
    count: Int,
    before: Commit? = null
  ): List<Commit>

  suspend fun sendMessage(
    text: String,
    parentMessage: Commit.Message?
  ): Commit.Message

  suspend fun markAsRead()
}
