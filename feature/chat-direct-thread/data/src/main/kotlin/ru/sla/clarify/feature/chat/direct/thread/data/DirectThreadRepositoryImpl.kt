package ru.sla.clarify.feature.chat.direct.thread.data

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
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.chat.ChatCommit
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.direct.thread.data.common.ThreadMediator
import ru.sla.clarify.feature.chat.direct.thread.data.mapper.mapToPeer
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadRepository
import ru.sla.clarify.feature.chat.direct.thread.domain.di.DirectThreadScope
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.entity.UserNM
import ru.sla.clarify.lib.google.firestore.toEpochMillis
import ru.sla.clarify.mapper.data.lastCommitWriteAfterDeleting
import ru.sla.clarify.mapper.data.mapToBranch
import ru.sla.clarify.mapper.data.mapToCommit
import ru.sla.clarify.mapper.data.mapToMember
import ru.sla.clarify.mapper.data.toDomain
import ru.sla.clarify.mapper.data.toLocalDateTime
import ru.sla.clarify.mapper.data.unreadDelta
import ru.sla.clarify.mapper.data.withReadStatus
import ru.sla.log.log
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.time.LocalDateTime

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
    val peerUserId = UserId(peerId.value)
    firestore.userLive(peerUserId)
      .filterNotNull()
      .collect(::applyPeerChanges)
  }

  override suspend fun subscribeOnCommitChanges() {
    val userId = threadMediator.requireUserId()
    val conversationId = threadMediator.awaitConversationId()
    firestore.directCommitsLive(
      peerId = peerId.value,
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
    firestore.branchesLive(
      conversationId = conversationId
    ).collect { changes ->
      applyBranchesChanges(
        changes = changes
      )
    }
  }

  override suspend fun subscribeOnBranchesUnreadCounts() {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.chatBranchQueries
      .selectIdsByConversationId(conversationId)
      .observeList()
      .collectLatest(::subscribeOnBranchUnreadCount)
  }

  private suspend fun subscribeOnBranchUnreadCount(ids: List<String>) {
    return coroutineScope {
      val conversationId = threadMediator.awaitConversationId()
      ids.forEach { branchId ->
        launch {
          firestore.branchUnreadCountLive(
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
    val historyCommits = firestore.readCommits(
      conversationId = conversationId,
      branchId = conversationId,
      limit = count.toLong(),
      before = before?.timestamp
    )
    applyInsertOrReplaceCommits(
      commits = historyCommits
    )
  }

  override suspend fun sendCommit(text: String) {
    firestore.createDirectCommit(
      conversationId = threadMediator.conversationId(),
      text = text,
      peerId = peerId.value,
      branchId = null
    )
  }

  override suspend fun markAsRead() {
    val conversationId = threadMediator.conversationId() ?: return
    firestore.updateUnreadCount(conversationId)
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    val conversationId = threadMediator.conversationId() ?: return
    val current = lastReadWatermark.value
    if (current != null && !lastReadAt.isAfter(current)) {
      log { "Direct: lastReadAt ($lastReadAt) is not after current watermark ($current), skipping" }
      return
    }
    lastReadWatermark.value = lastReadAt
    firestore.updateReadWatermark(conversationId, lastReadAt)
    firestore.updateUnreadCount(conversationId)
  }

  override suspend fun createBranch(
    parentId: Branch.Id?,
    from: Commit.Id,
    name: String
  ): Branch.Id {
    return withContext(Dispatchers.IO) {
      val conversationId = threadMediator.requireConversationId()
      val remote = firestore.createBranch(
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

  override suspend fun deleteCommits(ids: List<Commit.Id>, forEveryone: Boolean) {
    val conversationId = threadMediator.requireConversationId()
    if (forEveryone) {
      deleteCommitsForEveryone(ids)
    } else {
      firestore.hideCommits(
        conversationId = conversationId,
        commitIds = ids.map { it.value }
      )
    }
  }

  private suspend fun deleteCommitsForEveryone(ids: List<Commit.Id>) {
    return withContext(Dispatchers.IO) {
      val conversationId = threadMediator.awaitConversationId()
      val rootCommits = persistedDB.chatCommitQueries
        .selectByBranchId(conversationId, conversationId, ::mapToCommit)
        .executeAsList()
      val peerMember = firestore.readMember(
        conversationId = conversationId,
        memberId = peerId.value
      )
      val peerLastReadAt = peerMember?.lastReadAt
        ?.toEpochMillis()
        ?.toLocalDateTime()

      val deletedIds = ids.toSet()

      firestore.deleteDirectCommits(
        conversationId = conversationId,
        peerId = peerId.value,
        commitIds = ids.map { it.value },
        lastCommit = rootCommits.lastCommitWriteAfterDeleting(deletedIds),
        peerUnreadDelta = rootCommits.unreadDelta(deletedIds, peerLastReadAt)
      )
    }
  }

  override val peer: Flow<Peer?> = persistedDB.userQueries
    .selectById(peerId.value, ::mapToPeer)
    .observeOneOrNull()

  override val commits: Flow<List<Commit>> = flow {
    val conversationId = threadMediator.awaitConversationId()

    val commitsFlow = persistedDB.chatCommitQueries
      .selectByBranchId(conversationId, conversationId, ::mapToCommit)
      .observeList()

    val peerReadAtFlow = firestore.memberLive(
      conversationId = conversationId,
      memberId = peerId.value
    ).map { member ->
      member
        ?.lastReadAt
        ?.toEpochMillis()
        ?.toLocalDateTime()
    }

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

  override val unreadCount: Flow<Long> = flow {
    val conversationId = threadMediator.awaitConversationId()
    firestore.unreadCountLive(conversationId)
      .collect { emit(it) }
  }

  override val branches: Flow<List<Branch>> = flow {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.chatBranchQueries
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
      ChatCommit(
        id = commit.id,
        conversationId = conversationId,
        branchId = commit.branchId,
        senderId = commit.senderUid,
        type = commit.type.value,
        text = commit.text.orEmpty(),
        invitedUid = commit.invitedUid,
        timestamp = commit.createdAt?.toEpochMillis() ?: 0L,
        isSelf = commit.senderUid == userId.value,
        status = if (hasPendingWrites) {
          Commit.Status.Sending.value
        } else {
          Commit.Status.Sent.value
        }
      )
    )
  }

  private suspend fun applyBranchesChanges(changes: List<FirestoreChange<BranchNM>>) {
    return withContext(Dispatchers.IO) {
      val conversationId = threadMediator.awaitConversationId()
      persistedDB.transaction {
        changes.forEach { change ->
          when (change.changeType) {
            FirestoreDocumentResult.Removed -> {
              persistedDB.chatBranchQueries.deleteById(change.data.id)
            }

            FirestoreDocumentResult.Added,
            FirestoreDocumentResult.Modified -> {
              applyInsertOrReplaceBranch(change.data.toDomain(conversationId))
            }
          }
        }
      }
    }
  }

  private fun applyInsertOrReplaceBranch(branch: Branch) {
    persistedDB.chatBranchQueries.insertOrReplace(
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

  private suspend fun applyUpdateBranchUnreadCount(branchId: String, unreadCount: Long) {
    return withContext(Dispatchers.IO) {
      persistedDB.chatBranchQueries.updateUnreadCount(
        id = branchId,
        unreadCount = unreadCount
      )
    }
  }

  private suspend fun resolveBranchId(branchId: Branch.Id?): String {
    return branchId?.value ?: threadMediator.conversationId() ?: error("conversationId not found")
  }
}

private const val LIVE_COMMIT_LIMIT = 50L
