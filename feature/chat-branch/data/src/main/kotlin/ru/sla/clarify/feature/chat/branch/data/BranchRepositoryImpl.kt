package ru.sla.clarify.feature.chat.branch.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chat.branch.data.mapper.mapToBranch
import ru.sla.clarify.feature.chat.branch.data.mapper.toDomain
import ru.sla.clarify.feature.chat.branch.data.mapper.toLocalDateTime
import ru.sla.clarify.feature.chat.branch.data.mapper.withReadStatus
import ru.sla.clarify.feature.chat.branch.domain.BranchRepository
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.toEpochSeconds
import ru.sla.clarify.mapper.mapToCommit
import ru.sla.clarify.mapper.mapToMember
import ru.sla.clarify.mapper.mapToUser
import java.time.LocalDateTime
import javax.inject.Inject

@SingleIn(BranchScope::class)
@ContributesBinding(BranchScope::class)
class BranchRepositoryImpl @Inject constructor(
  params: TargetParams,
  private val firestore: Firestore,
  private val persistedDB: PersistedDB,
  private val authSessionPersistence: AuthSessionPersistence
) : BranchRepository {

  private val branchId = params.branchId
  private val lastReadWatermark = MutableStateFlow<LocalDateTime?>(null)

  override suspend fun subscribeOnBranchesChanges() {
    val conversationId = awaitConversationId()
    firestore.observeBranches(
      conversationId = conversationId
    ).flowOn(
      context = Dispatchers.IO
    ).collect { changes ->
      applyBranchesChanges(
        conversationId = conversationId,
        changes = changes
      )
    }
  }

  override suspend fun subscribeOnBranchCommitsChanges() {
    val userId = requireUserId()
    val conversationId = awaitConversationId()
    firestore.observeCommits(
      conversationId = conversationId,
      branchId = branchId.value,
      limit = LIVE_COMMIT_LIMIT
    ).flowOn(
      context = Dispatchers.IO
    ).collect { changes ->
      applyCommitChanges(
        conversationId = conversationId,
        userId = userId,
        changes = changes
      )
    }
  }

  override suspend fun subscribeOnBranchUnreadCount() {
    val conversationId = awaitConversationId()
    firestore.observeBranchUnreadCount(
      conversationId = conversationId,
      branchId = branchId.value
    ).flowOn(
      context = Dispatchers.IO
    ).collect { unreadCount ->
      applyUpdateUnreadCount(
        branchId = branchId.value,
        unreadCount = unreadCount
      )
    }
  }

  override suspend fun fetchHistoryCommits(count: Int, before: Commit?) {
    return withContext(Dispatchers.IO) {
      val conversationId = awaitConversationId()
      val historyCommits = firestore.getCommits(
        conversationId = conversationId,
        branchId = branchId.value,
        count = count,
        before = before?.timestamp
      )
      applyInsertOrReplaceCommits(conversationId, historyCommits)
    }
  }

  override suspend fun sendCommit(text: String) {
    return withContext(Dispatchers.IO) {
      val conversationId = requireConversationId()
      firestore.postBranchCommit(
        conversationId = conversationId,
        branchId = branchId.value,
        text = text,
        memberUids = memberUids(conversationId)
      )
    }
  }

  override suspend fun markAsRead() {
    return withContext(Dispatchers.IO) {
      firestore.patchBranchClearUnreadCount(
        branchId = branchId.value,
        conversationId = requireConversationId()
      )
    }
  }

  override suspend fun markReadUpTo(lastReadAt: LocalDateTime) {
    return withContext(Dispatchers.IO) {
      val conversationId = requireConversationId()
      val current = lastReadWatermark.value
      if (current != null && !lastReadAt.isAfter(current)) {
        return@withContext
      }
      lastReadWatermark.value = lastReadAt
      firestore.patchReadWatermark(
        conversationId = conversationId,
        lastReadAt = lastReadAt
      )
      firestore.patchBranchClearUnreadCount(
        branchId = branchId.value,
        conversationId = conversationId
      )
    }
  }

  override suspend fun openMergeRequest() {
    return withContext(Dispatchers.IO) {
      firestore.postMergeRequest(
        branchId = branchId.value,
        conversationId = requireConversationId()
      )
    }
  }

  override suspend fun approveMergeRequest() {
    return withContext(Dispatchers.IO) {
      val conversationId = requireConversationId()
      firestore.patchMergeApproval(
        branchId = branchId.value,
        conversationId = conversationId,
        memberUids = memberUids(conversationId)
      )
    }
  }

  override suspend fun revokeApprovalMergeRequest() {
    return withContext(Dispatchers.IO) {
      firestore.deleteMergeApproval(
        branchId = branchId.value,
        conversationId = requireConversationId()
      )
    }
  }

  override suspend fun cancelMergeRequest() {
    return withContext(Dispatchers.IO) {
      firestore.deleteMergeRequest(
        branchId = branchId.value,
        conversationId = requireConversationId()
      )
    }
  }

  override suspend fun finalizeMergeRequest() {
    return withContext(Dispatchers.IO) {
      firestore.patchMergeFinalize(
        branchId = branchId.value,
        conversationId = requireConversationId()
      )
    }
  }

  override val user: Flow<User?> = flow {
    val userId = authSessionPersistence.withKey { readUserId(it) }
    if (userId == null) return@flow emit(null)
    persistedDB.userQueries
      .selectById(userId.value, ::mapToUser)
      .observeOneOrNull()
      .flowOn(Dispatchers.IO)
      .collect { emit(it) }
  }

  override val branch: Flow<Branch?> = persistedDB.branchQueries
    .selectById(branchId.value, ::mapToBranch)
    .observeOneOrNull()

  override val commits: Flow<List<Commit>> = flow {
    val conversationId = awaitConversationId()
    val selfId = requireUserId()

    val commitsFlow = persistedDB.chatCommitQueries
      .selectByBranchId(conversationId, branchId.value, ::mapToCommit)
      .observeList()

    val peerReadAtFlow = peerReadAtFlow(conversationId, selfId)

    val result = combine(
      flow = commitsFlow,
      flow2 = peerReadAtFlow
    ) { commits, peerReadAt ->
      commits.map { it.withReadStatus(peerReadAt) }
    }

    emitAll(result)
  }

  override val unreadCount: Flow<Long> = flow {
    val conversationId = awaitConversationId()
    firestore.observeBranchUnreadCount(
      branchId = branchId.value,
      conversationId = conversationId
    ).collect { emit(it) }
  }

  override val members: Flow<List<Member>> = flow {
    val conversationId = awaitConversationId()
    persistedDB.chatConversationMemberQueries
      .selectByConversation(conversationId, ::mapToMember)
      .observeList()
      .collect { emit(it) }
  }

  override fun member(id: UserId): Flow<Member?> = flow {
    val conversationId = awaitConversationId()
    persistedDB.chatConversationMemberQueries
      .selectByConversationAndId(conversationId, id.value, ::mapToMember)
      .observeOneOrNull()
      .collect { emit(it) }
  }

  /**
   * Read-watermark пира на уровне conversation (read-receipts общие для всей переписки,
   * включая ветки). Branch direct-only — участников ровно двое, пир тот, чей id != self.
   */
  private fun peerReadAtFlow(
    conversationId: String,
    selfId: UserId
  ): Flow<LocalDateTime?> {
    val peerUid = persistedDB.chatConversationMemberQueries
      .selectByConversation(conversationId, ::mapToMember)
      .executeAsList()
      .firstOrNull { it.id.value != selfId.value }
      ?.id
      ?.value
    return if (peerUid == null) {
      flowOf(null)
    } else {
      firestore.observeMember(conversationId, UserId(peerUid))
        .map { it?.lastReadAt?.toLocalDateTime() }
    }
  }

  private fun memberUids(conversationId: String): List<String> {
    return persistedDB.chatConversationMemberQueries
      .selectByConversation(conversationId, ::mapToMember)
      .executeAsList()
      .map { it.id.value }
  }

  private fun applyBranchesChanges(
    conversationId: String,
    changes: List<FirestoreChange<BranchNM>>
  ) {
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

  private fun applyUpdateUnreadCount(branchId: String, unreadCount: Long) {
    persistedDB.branchQueries.updateUnreadCount(
      id = branchId,
      unreadCount = unreadCount
    )
  }

  private suspend fun applyInsertOrReplaceCommits(
    conversationId: String,
    commits: List<CommitNM>
  ) {
    withContext(Dispatchers.IO) {
      val userId = requireUserId()
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

  // Flow.first() — suspend; detekt с type resolution ошибочно считает suspend избыточным.
  @Suppress("RedundantSuspendModifier")
  private suspend fun awaitConversationId(): String {
    return persistedDB.branchQueries
      .selectById(branchId.value, ::mapToBranch)
      .observeOneOrNull()
      .filterNotNull()
      .first()
      .conversationId
      .value
  }

  private fun requireConversationId(): String {
    return requireNotNull(
      persistedDB.branchQueries
        .selectById(branchId.value, ::mapToBranch)
        .executeAsOneOrNull()
        ?.conversationId
        ?.value
    ) {
      "conversationId not found for branch ${branchId.value}"
    }
  }

  private suspend fun requireUserId(): UserId {
    return requireNotNull(authSessionPersistence.withKey { readUserId(it) })
  }
}

private const val LIVE_COMMIT_LIMIT = 50L
