package ru.sla.clarify.feature.chat.group.thread.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.entity.FoundUser
import ru.sla.clarify.feature.chat.conversation.domain.entity.Group
import ru.sla.clarify.feature.chat.conversation.domain.entity.GroupMember
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.domain.entity.GroupThreadTarget
import ru.sla.clarify.feature.entity.chat.Commit
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(GroupThreadScope::class)
class GroupThreadModel @Inject constructor(
  target: GroupThreadTarget,
  private val groupThreadRepository: GroupThreadRepository,
  private val conversationRepository: ConversationRepository
) : ReactiveModel() {

  private val conversationId = target.conversationId

  override fun onPostStart() {
    super.onPostStart()
    scope.launch { groupThreadRepository.subscribeOnCommitChanges() }
    scope.launch { conversationRepository.subscribeOnGroupParticipants(conversationId) }
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
    conversationRepository.renameGroup(id = conversationId, name = name)
  }

  val deleteGroup = task<Unit>(
    name = "deleteGroup"
  ) {
    conversationRepository.deleteGroup(conversationId)
  }

  val leaveGroup = task<Unit>(
    name = "leaveGroup"
  ) {
    conversationRepository.leaveGroup(conversationId)
  }

  val inviteMembers = task<List<UserId>, Unit>(
    name = "inviteMembers"
  ) { userIds ->
    conversationRepository.inviteGroupMembers(id = conversationId, userIds = userIds)
  }

  val removeMember = task<UserId, Unit>(
    name = "removeMember"
  ) { userId ->
    conversationRepository.removeGroupMember(id = conversationId, userId = userId)
  }

  val searchUsers = task<String, List<FoundUser>>(
    name = "searchUsers"
  ) { prefix ->
    conversationRepository.searchMemberByPrefix(prefix)
  }

  val user: Flow<User?> = conversationRepository.user
  val group: Flow<Group?> = conversationRepository.observeGroup(conversationId)
  val members: Flow<List<GroupMember>> = conversationRepository.observeGroupMembers(conversationId)
  val commits: Flow<List<Commit>> = groupThreadRepository.commits
  val unreadCount: Flow<Long> = groupThreadRepository.unreadCount
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
