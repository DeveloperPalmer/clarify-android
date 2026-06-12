package ru.sla.clarify.feature.chat.thread.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.mapDistinctNotNullChanges
import ru.sla.clarify.feature.chat.conversation.domain.ConversationRepository
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.conversation.domain.entity.FoundUser
import ru.sla.clarify.feature.chat.conversation.domain.entity.Group
import ru.sla.clarify.feature.chat.conversation.domain.entity.GroupMember
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
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
    scope.launch { threadRepository.subscribeOnCommitChanges(null) }
    scope.launch { branchRepository.subscribeOnBranchChanges() }
    scope.launch { branchRepository.subscribeOnBranchesUnreadCounts() }
  }

  fun subscribeOnCommitChanges(branchId: Branch.Id) {
    scope.launch { threadRepository.subscribeOnCommitChanges(branchId) }
  }

  fun commits(branchId: Branch.Id?): Flow<List<Commit>> {
    return threadRepository.commits(branchId)
  }

  fun markReadCommits() {
    scope.launch { threadRepository.markAsRead() }
  }

  fun markReadCommits(branchId: Branch.Id) {
    scope.launch { branchRepository.markAsRead(branchId) }
  }

  fun markReadUpTo(lastReadAt: LocalDateTime) {
    scope.launch { threadRepository.markReadUpTo(lastReadAt) }
  }

  val fetchHistoryCommits = task<Unit>(
    name = "fetchHistoryCommits"
  ) {
    threadRepository.fetchHistoryCommits(
      branchId = null,
      count = DEFAULT_HISTORY_PAGE_SIZE
    )
  }

  val fetchHistoryBranchCommits = task<Branch.Id, Unit>(
    name = "fetchHistoryBranchCommits"
  ) { branchId ->
    threadRepository.fetchHistoryCommits(
      branchId = branchId,
      count = DEFAULT_HISTORY_PAGE_SIZE
    )
  }

  val sendMessage = task<Branch.Id?, String, String?, Unit>(
    name = "sendMessage"
  ) { branchId, text, colorHex ->
    threadRepository.sendCommit(
      branchId = branchId,
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

  val openMergeRequest = task<Branch.Id, Unit>(
    name = "openMergeRequest"
  ) { branchId ->
    branchRepository.openMergeRequest(branchId)
  }

  val approveMergeRequest = task<Branch.Id, Unit>(
    name = "approveMergeRequest"
  ) { branchId ->
    branchRepository.approveMergeRequest(branchId)
  }

  val revokeApprovalMergeRequest = task<Branch.Id, Unit>(
    name = "revokeApprovalMergeRequest"
  ) { branchId ->
    branchRepository.revokeApprovalMergeRequest(branchId)
  }

  val cancelMergeRequest = task<Branch.Id, Unit>(
    name = "cancelMergeRequest"
  ) { branchId ->
    branchRepository.cancelMergeRequest(branchId)
  }

  val finalizeMergeRequest = task<Branch.Id, Unit>(
    name = "finalizeMergeRequest"
  ) { branchId ->
    branchRepository.finalizeMergeRequest(branchId)
  }

  fun subscribeOnGroupParticipants(id: Conversation.Id) {
    scope.launch { conversationRepository.subscribeOnGroupParticipants(id) }
  }

  fun group(id: Conversation.Id): Flow<Group?> = conversationRepository.observeGroup(id)

  fun groupMembers(id: Conversation.Id): Flow<List<GroupMember>> =
    conversationRepository.observeGroupMembers(id)

  val renameGroup = task<Conversation.Id, String, Unit>(
    name = "renameGroup"
  ) { id, name ->
    conversationRepository.renameGroup(id = id, name = name)
  }

  val deleteGroup = task<Conversation.Id, Unit>(
    name = "deleteGroup"
  ) { id ->
    conversationRepository.deleteGroup(id)
  }

  val leaveGroup = task<Conversation.Id, Unit>(
    name = "leaveGroup"
  ) { id ->
    conversationRepository.leaveGroup(id)
  }

  val inviteMembers = task<Conversation.Id, List<UserId>, Unit>(
    name = "inviteMembers"
  ) { id, userIds ->
    conversationRepository.inviteMembers(id = id, userIds = userIds)
  }

  val removeMember = task<Conversation.Id, UserId, Unit>(
    name = "removeMember"
  ) { id, userId ->
    conversationRepository.removeMember(id = id, userId = userId)
  }

  val searchUsers = task<String, List<FoundUser>>(
    name = "searchUsers"
  ) { prefix ->
    conversationRepository.searchUsersByEmailPrefix(prefix)
  }

  val user: Flow<User?> = conversationRepository.user
  val peer: Flow<Peer?> = threadRepository.peer

  val unreadCount: Flow<Long> = threadRepository.unreadCount

  val participants: Flow<List<Participant>> = threadRepository.participants

  val branches: Flow<List<Branch>> = branchRepository.branches
  fun branch(id: Branch.Id): Flow<Branch?> = branchRepository.branch(id)

  fun mergeRequestInitiator(id: Branch.Id): Flow<Participant?> = branchRepository.branch(id)
    .mapDistinctNotNullChanges { it?.mergeRequest?.initiatorId }
    .flatMapLatest(threadRepository::participant)
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
