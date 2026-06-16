package ru.sla.clarify.feature.chat.direct.thread.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.feature.chat.direct.thread.data.common.ThreadMediator
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.mapToBranch
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.mapToCommit
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.mapToMember
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.mapToPeer
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.toDomain
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.toLocalDateTime
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.withReadStatus
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadRepository
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams
import ru.sla.clarify.feature.entity.chat.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Member
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.entity.UserNM
import ru.sla.clarify.lib.google.firestore.toEpochSeconds
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(DirectThreadScope::class)
@ContributesBinding(DirectThreadScope::class)
class DirectThreadRepositoryImpl @Inject constructor(
  params: TargetParams,
  private val firestore: Firestore,
  private val persistedDB: PersistedDB,
  private val threadMediator: ThreadMediator
) : DirectThreadRepository {

  private val peerId = params.peerId
  private val lastReadWatermark = MutableStateFlow<LocalDateTime?>(null)

  override suspend fun subscribeOnPeerChanges() {
    val peerId = UserId(peerId.value)
    firestore.observeUser(peerId)
      .filterNotNull()
      .collect(::applyPeerChanges)
  }

  override suspend fun subscribeOnCommitChanges() {
    val userId = threadMediator.requireUserId()
    val conversationId = threadMediator.awaitConversationId()
    firestore.observeDirectCommits(
      peerId = peerId,
      branchId = conversationId,
      limit = LIVE_COMMIT_LIMIT
    ).collect { changes ->
      applyCommitChanges(
        conversationId = conversationId,
        userId = userId,
        changes = changes
      )
    }
  }

  override suspend fun subscribeOnBranchesChanges() {
    val conversationId = threadMediator.awaitConversationId()
    firestore.observeBranches(
      conversationId = conversationId
    ).collect { changes ->
      applyBranchesChanges(
        changes = changes
      )
    }
  }

  override suspend fun subscribeOnBranchesUnreadCounts() {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.branchQueries
      .selectIdsByConversationId(conversationId)
      .observeList()
      .collectLatest(::subscribeOnBranchUnreadCount)
  }

  private suspend fun subscribeOnBranchUnreadCount(ids: List<String>) {
    return coroutineScope {
      val conversationId = threadMediator.awaitConversationId()
      ids.forEach { branchId ->
        launch {
          firestore.observeBranchUnreadCount(
            conversationId = conversationId,
            branchId = branchId
          ).collect { unreadCount ->
            applyUpdateBranchUnreadCount(
              branchId = branchId,
              unreadCount = unreadCount
            )
          }
        }
      }
    }
  }

  override suspend fun fetchHistoryCommits(
    count: Int,
    before: Commit?
  ) {
    val conversationId = threadMediator.conversationId() ?: return
    val historyCommits = firestore.getCommits(
      conversationId = conversationId,
      branchId = conversationId,
      count = count,
      before = before?.timestamp
    )
    applyInsertOrReplaceCommits(
      commits = historyCommits
    )
  }

  override suspend fun sendCommit(text: String) {
    firestore.postCommit(
      conversationId = threadMediator.conversationId(),
      text = text,
      peerId = peerId,
      branchId = null
    )
  }

  override suspend fun markAsRead() {
    val conversationId = threadMediator.conversationId() ?: return
    firestore.patchClearUnreadCount(conversationId)
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    val conversationId = threadMediator.conversationId() ?: return
    val current = lastReadWatermark.value
    if (current != null && !lastReadAt.isAfter(current)) {
      return
    }
    lastReadWatermark.value = lastReadAt
    firestore.patchReadWatermark(conversationId, lastReadAt)
    firestore.patchClearUnreadCount(conversationId)
  }

  override suspend fun createBranch(parentId: Branch.Id?, from: Commit.Id, name: String): Branch.Id {
    return withContext(Dispatchers.IO) {
      val conversationId = threadMediator.requireConversationId()
      val remote = firestore.postBranch(
        conversationId = conversationId,
        parentBranchId = resolveBranchId(parentId),
        branchedFromCommitId = from.value,
        name = name
      )
      val branch = remote.toDomain(conversationId)
      applyInsertOrReplaceBranch(branch)
      branch.id
    }
  }

  override val peer: Flow<Peer?> = persistedDB.userQueries
    .selectById(peerId.value, ::mapToPeer)
    .observeOneOrNull()

  override val commits: Flow<List<Commit>> = flow {
    val peerId = UserId(peerId.value)
    val conversationId = threadMediator.awaitConversationId()

    val commitsFlow = persistedDB.chatCommitQueries
      .selectByBranchId(conversationId, conversationId, ::mapToCommit)
      .observeList()

    val peerReadAtFlow = firestore
      .observeMember(conversationId, peerId)
      .map { it?.lastReadAt?.toLocalDateTime() }

    combine(
      flow = commitsFlow,
      flow2 = peerReadAtFlow
    ) { commits, peerReadAt ->
      commits.map { it.withReadStatus(peerReadAt) }
    }.collect { emit(it) }
  }

  override val members: Flow<List<Member>> = flow {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.chatConversationMemberQueries
      .selectByConversation(conversationId, ::mapToMember)
      .observeList()
      .collect { emit(it) }
  }

  override fun member(initiator: UserId): Flow<Member?> = flow {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.chatConversationMemberQueries
      .selectByConversationAndId(conversationId, initiator.value, ::mapToMember)
      .observeOneOrNull()
      .collect { emit(it) }
  }

  override val unreadCount: Flow<Long> = flow {
    val conversationId = threadMediator.awaitConversationId()
    firestore.observeUnreadCount(conversationId)
      .collect { emit(it) }
  }

  override val branches: Flow<List<Branch>> = flow {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.branchQueries
      .selectByConversationId(conversationId, ::mapToBranch)
      .observeList()
      .collect { emit(it) }
  }

  private suspend fun applyInsertOrReplaceCommits(commits: List<CommitNM>) {
    return withContext(Dispatchers.IO) {
      val userId = threadMediator.requireUserId()
      val conversationId = threadMediator.awaitConversationId()
      persistedDB.transaction {
        commits.forEach { item ->
          applyInsertOrReplaceCommit(
            conversationId = conversationId,
            commit = item,
            userId = userId,
            hasPendingWrites = false
          )
        }
      }
    }
  }

  private suspend fun applyPeerChanges(user: UserNM): Unit = withContext(Dispatchers.IO) {
    persistedDB.userQueries.insertOrReplace(
      id = user.id,
      email = user.email,
      displayName = user.displayName,
      photoUrl = user.photoUrl
    )
  }

  private suspend fun applyCommitChanges(
    conversationId: String,
    userId: UserId,
    changes: List<FirestoreChange<CommitNM>>
  ): Unit = withContext(Dispatchers.IO) {
    persistedDB.transaction {
      changes.forEach { change ->
        val commit = change.data
        when (change.changeType) {
          FirestoreDocumentResult.Removed -> {
            persistedDB.chatCommitQueries.deleteById(commit.id)
          }
          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            applyInsertOrReplaceCommit(
              conversationId = conversationId,
              commit = commit,
              userId = userId,
              hasPendingWrites = change.hasPendingWrites
            )
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceCommit(
    conversationId: String,
    commit: CommitNM,
    userId: UserId,
    hasPendingWrites: Boolean
  ) {
    persistedDB.chatCommitQueries.insertOrReplace(
      id = commit.id,
      conversationId = conversationId,
      branchId = commit.branchId,
      senderId = commit.senderUid,
      type = commit.type.value,
      text = commit.text.orEmpty(),
      invitedUid = commit.invitedUid,
      timestamp = commit.createdAt?.toEpochSeconds() ?: 0L,
      isSelf = commit.senderUid == userId.value,
      status = if (hasPendingWrites) {
        Commit.Status.Sending.value
      } else {
        Commit.Status.Sent.value
      }
    )
  }

  private suspend fun applyBranchesChanges(changes: List<FirestoreChange<BranchNM>>) {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.transaction {
      changes.forEach { change ->
        when (change.changeType) {
          FirestoreDocumentResult.Removed -> {
            persistedDB.branchQueries.deleteById(change.data.id)
          }
          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            applyInsertOrReplaceBranch(change.data.toDomain(conversationId))
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceBranch(branch: Branch) {
    persistedDB.branchQueries.insertOrReplace(
      id = branch.id.value,
      conversationId = branch.conversationId.value,
      parentBranchId = branch.parentBranchId.value,
      branchedFromCommitId = branch.branchedFromCommitId.value,
      name = branch.name,
      lastCommit = branch.lastCommit,
      lastCommitTimestamp = branch.lastCommitTimestamp,
      createdAt = branch.createdAt,
      createdByUid = branch.createdById.value
    )
    val mergeRequest = branch.mergeRequest
    if (mergeRequest != null) {
      persistedDB.mergeRequestQueries.insertOrReplace(
        branchId = branch.id.value,
        status = mergeRequest.status.value,
        initiatorUid = mergeRequest.initiatorId.value,
        requestedAt = mergeRequest.requestedAt,
        approvedByUids = mergeRequest.approvedByIds.map { it.value },
        mergedAt = mergeRequest.mergedAt,
        mergedIntoBranchId = mergeRequest.mergedIntoBranchId?.value
      )
    } else {
      persistedDB.mergeRequestQueries.deleteByBranchId(branch.id.value)
    }
  }

  private fun applyUpdateBranchUnreadCount(branchId: String, unreadCount: Long) {
    persistedDB.branchQueries.updateUnreadCount(
      id = branchId,
      unreadCount = unreadCount
    )
  }

  private suspend fun resolveBranchId(branchId: Branch.Id?): String {
    return branchId?.value ?: threadMediator.conversationId() ?: error("conversationId not found")
  }
}

private const val LIVE_COMMIT_LIMIT = 50L
