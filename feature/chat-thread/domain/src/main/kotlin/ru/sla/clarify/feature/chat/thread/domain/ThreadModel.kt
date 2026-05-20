package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.entity.chat.Commit
import javax.inject.Inject

@SingleIn(ThreadScope::class)
class ThreadModel @Inject constructor(
  private val threadRepository: ThreadRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    threadRepository.subscribeOnCommits()
      .launchIn(scope)
  }

  val fetchHistoryCommit = task<Unit>(name = "fetchHistoryCommit") {
    threadRepository.fetchHistoryCommits(count = DEFAULT_HISTORY_PAGE_SIZE)
  }

  val sendMessage = task<String, Commit.Message?, Unit>(
    name = "sendMessage"
  ) { text, parent ->
    threadRepository.sendCommit(
      text = text,
      parent = parent
    )
  }

  fun markReadCommits() {
    scope.launch { threadRepository.markAsRead() }
  }

  val commits: Flow<Commit> = threadRepository.commits
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
