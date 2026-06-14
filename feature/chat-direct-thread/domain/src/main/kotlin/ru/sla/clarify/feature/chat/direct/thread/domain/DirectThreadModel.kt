package ru.sla.clarify.feature.chat.direct.thread.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.entity.chat.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Member
import ru.sla.clarify.feature.entity.chat.Peer
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(DirectThreadScope::class)
class DirectThreadModel @Inject constructor(
  private val directThreadRepository: DirectThreadRepository,
  private val conversationRepository: ConversationRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    scope.launch { directThreadRepository.subscribeOnPeerChanges() }
    scope.launch { directThreadRepository.subscribeOnCommitChanges() }
    scope.launch { directThreadRepository.subscribeOnBranchesChanges() }
    scope.launch { directThreadRepository.subscribeOnBranchesUnreadCounts() }
  }

  fun markReadCommits() {
    scope.launch { directThreadRepository.markAsRead() }
  }

  fun markReadUpTo(lastReadAt: LocalDateTime) {
    scope.launch { directThreadRepository.markReadUpTo(lastReadAt) }
  }

  val fetchHistoryCommits = task<Unit>(
    name = "fetchHistoryCommits"
  ) {
    directThreadRepository.fetchHistoryCommits(
      count = DEFAULT_HISTORY_PAGE_SIZE
    )
  }

  val sendMessage = task<String, String?, Unit>(
    name = "sendMessage"
  ) { text, colorHex ->
    directThreadRepository.sendCommit(
      text = text,
      colorHex = colorHex
    )
  }

  val createBranch = task<Branch.Id?, Commit.Message, String, Branch>(
    name = "createBranch"
  ) { parentBranchId, branchedFromCommitId, name ->
    directThreadRepository.createBranch(
      parentId = parentBranchId,
      from = branchedFromCommitId.id,
      name = name
    )
  }

  val user: Flow<User?> = conversationRepository.user
  val peer: Flow<Peer?> = directThreadRepository.peer

  val commits: Flow<List<Commit>> = directThreadRepository.commits
  val unreadCount: Flow<Long> = directThreadRepository.unreadCount

  val members: Flow<List<Member>> = directThreadRepository.members

  val branches: Flow<List<Branch>> = directThreadRepository.branches
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
