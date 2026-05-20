package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.entity.chat.Commit
import javax.inject.Inject

@SingleIn(ThreadScope::class)
class ThreadModel @Inject constructor(
  private val threadRepository: ThreadRepository
) : ReactiveModel() {

  val getCommitHistory = task<List<Commit>>(
    name = "getCommitHistory"
  ) {
    threadRepository.getCommitHistory(count = DEFAULT_HISTORY_PAGE_SIZE)
  }

  val sendMessage = task<String, Commit.Message?, Commit.Message>(
    name = "sendMessage"
  ) { text, parentMessage ->
    threadRepository.sendMessage(
      text = text,
      parentMessage = parentMessage
    )
  }

  fun markReadCommits() {
    scope.launch { threadRepository.markAsRead() }
  }

  suspend fun getConversation(): Conversation? {
    return threadRepository.getConversation()
  }

  fun peerCommits(): Flow<Commit> {
    return threadRepository.peerCommits()
  }
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
