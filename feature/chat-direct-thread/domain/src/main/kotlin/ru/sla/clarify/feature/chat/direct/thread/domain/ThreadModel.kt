package ru.sla.clarify.feature.chat.direct.thread.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.direct.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Participant
import ru.sla.clarify.feature.entity.chat.Peer
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(ThreadScope::class)
class ThreadModel @Inject constructor(
  private val threadRepository: ThreadRepository,
  private val branchRepository: BranchRepository,
  private val conversationRepository: ConversationRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    scope.launch { threadRepository.subscribeOnPeerChanges() }
    scope.launch { threadRepository.subscribeOnCommitChanges() }
    scope.launch { branchRepository.subscribeOnBranchChanges() }
    scope.launch { branchRepository.subscribeOnBranchesUnreadCounts() }
  }

  fun commits(): Flow<List<Commit>> {
    return threadRepository.commits()
  }

  fun markReadCommits() {
    scope.launch { threadRepository.markAsRead() }
  }

  fun markReadUpTo(lastReadAt: LocalDateTime) {
    scope.launch { threadRepository.markReadUpTo(lastReadAt) }
  }

  val fetchHistoryCommits = task<Unit>(
    name = "fetchHistoryCommits"
  ) {
    threadRepository.fetchHistoryCommits(
      count = DEFAULT_HISTORY_PAGE_SIZE
    )
  }

  val sendMessage = task<String, String?, Unit>(
    name = "sendMessage"
  ) { text, colorHex ->
    threadRepository.sendCommit(
      text = text,
      colorHex = colorHex
    )
  }

  val createBranch = task<Branch.Id?, Commit.Message, String, Branch>(
    name = "createBranch"
  ) { parentBranchId, branchedFromCommitId, name ->
    branchRepository.createBranch(
      parentId = parentBranchId,
      from = branchedFromCommitId.id,
      name = name
    )
  }

  val user: Flow<User?> = conversationRepository.user
  val peer: Flow<Peer?> = threadRepository.peer

  val unreadCount: Flow<Long> = threadRepository.unreadCount

  val participants: Flow<List<Participant>> = threadRepository.participants

  val branches: Flow<List<Branch>> = branchRepository.branches
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
