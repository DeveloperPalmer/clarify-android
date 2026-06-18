package ru.sla.clarify.feature.chat.group.thread.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.domain.entity.FoundUser
import ru.sla.clarify.feature.chat.group.thread.domain.entity.Group
import ru.sla.clarify.feature.chat.group.thread.domain.entity.GroupMember
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(GroupThreadScope::class)
class GroupThreadModel @Inject constructor(
  private val groupThreadRepository: GroupThreadRepository,
  private val conversationRepository: ConversationRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    scope.launch { groupThreadRepository.subscribeOnCommitChanges() }
    scope.launch { groupThreadRepository.subscribeOnGroupMembers() }
  }

  fun markReadUpTo(lastReadAt: LocalDateTime) {
    scope.launch { groupThreadRepository.markReadUpTo(lastReadAt) }
  }

  val fetchHistoryCommits = task<Unit>(
    name = "fetchHistoryCommits"
  ) {
    groupThreadRepository.fetchHistoryCommits(count = DEFAULT_HISTORY_PAGE_SIZE)
  }

  val sendMessage = task<String, Unit>(
    name = "sendMessage"
  ) { text ->
    groupThreadRepository.sendCommit(text)
  }

  val renameGroup = task<String, Unit>(
    name = "renameGroup"
  ) { name ->
    groupThreadRepository.renameGroup(name = name)
  }

  val deleteGroup = task<Unit>(
    name = "deleteGroup"
  ) {
    groupThreadRepository.deleteConversation()
  }

  val leaveGroup = task<Unit>(
    name = "leaveGroup"
  ) {
    groupThreadRepository.leaveConversation()
  }

  val inviteMembers = task<List<UserId>, Unit>(
    name = "inviteMembers"
  ) { userIds ->
    groupThreadRepository.inviteGroupMembers(userIds = userIds)
  }

  val removeMember = task<UserId, Unit>(
    name = "removeMember"
  ) { userId ->
    groupThreadRepository.deleteConversationMember(userId = userId)
  }

  val searchUsers = task<String, List<FoundUser>>(
    name = "searchUsers"
  ) { prefix ->
    groupThreadRepository.searchMemberByPrefix(prefix = prefix)
  }

  val user: Flow<User?> = conversationRepository.user
  val group: Flow<Group?> = groupThreadRepository.observeGroup()
  val members: Flow<List<GroupMember>> = groupThreadRepository.observeGroupMembers()
  val commits: Flow<List<Commit>> = groupThreadRepository.commits
  val unreadCount: Flow<Long> = groupThreadRepository.unreadCount
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
