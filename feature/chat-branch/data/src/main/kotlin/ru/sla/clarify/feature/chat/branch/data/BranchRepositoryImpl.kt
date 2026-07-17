package ru.sla.clarify.feature.chat.branch.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.sla.clarify.auth.session.data.storage.AuthSessionPersistence
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.chat.ChatCommit
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chat.branch.domain.BranchRepository
import ru.sla.clarify.feature.chat.branch.domain.di.BranchScope
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.toEpochMillis
import ru.sla.clarify.mapper.data.mapToBranch
import ru.sla.clarify.mapper.data.mapToCommit
import ru.sla.clarify.mapper.data.mapToMember
import ru.sla.clarify.mapper.data.mapToUser
import ru.sla.clarify.mapper.data.toDomain
import ru.sla.clarify.mapper.data.toLocalDateTime
import ru.sla.clarify.mapper.data.withReadStatus
import ru.sla.log.log
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import java.time.LocalDateTime

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
    firestore.branchesLive(
      conversationId = conversationId
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
    firestore.commitsLive(
      conversationId = conversationId,
      branchId = branchId.value,
      limit = LIVE_COMMIT_LIMIT
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
    firestore.branchUnreadCountLive(
      conversationId = conversationId,
      branchId = branchId.value
    ).collect { unreadCount ->
      applyUpdateUnreadCount(
        branchId = branchId.value,
        unreadCount = unreadCount
      )
    }
  }

  override suspend fun fetchHistoryCommits(count: Int, before: Commit?) {
    val conversationId = awaitConversationId()
    val historyCommits = firestore.readCommits(
      conversationId = conversationId,
      branchId = branchId.value,
      limit = count.toLong(),
      before = before?.timestamp
    )
    applyInsertOrReplaceCommits(
      conversationId = conversationId,
      commits = historyCommits
    )
  }

  override suspend fun sendCommit(text: String) {
    return withContext(Dispatchers.IO) {
      val conversationId = requireConversationId()
      firestore.createBranchCommit(
        conversationId = conversationId,
        branchId = branchId.value,
        text = text,
        memberUids = memberUids(conversationId)
      )
    }
  }

  override suspend fun markAsRead() {
    return withContext(Dispatchers.IO) {
      firestore.updateBranchUnreadCount(
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
        log { "Branch: lastReadAt ($lastReadAt) is not after current watermark ($current), skipping" }
        return@withContext
      }
      lastReadWatermark.value = lastReadAt
      firestore.updateReadWatermark(
        conversationId = conversationId,
        lastReadAt = lastReadAt
      )
      firestore.updateBranchUnreadCount(
        branchId = branchId.value,
        conversationId = conversationId
      )
    }
  }

  override suspend fun openMergeRequest() {
    return withContext(Dispatchers.IO) {
      firestore.createOpenMergeRequest(
        branchId = branchId.value,
        conversationId = requireConversationId()
      )
    }
  }

  override suspend fun approveMergeRequest() {
    return withContext(Dispatchers.IO) {
      val conversationId = requireConversationId()
      firestore.updateMergeApproval(
        branchId = branchId.value,
        conversationId = conversationId,
        memberUids = memberUids(conversationId)
      )
    }
  }

  override suspend fun revokeMergeRequestApproval() {
    return withContext(Dispatchers.IO) {
      firestore.deleteMergeRequestApproval(
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
      firestore.updateMergeFinalize(
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
      .collect { emit(it) }
  }

  override val branch: Flow<Branch?> = persistedDB.chatBranchQueries
    .selectById(branchId.value, ::mapToBranch)
    .observeOneOrNull()

  override val commits: Flow<List<Commit>> = flow {
    val conversationId = awaitConversationId()
    val selfId = requireUserId()

    val commitsFlow = persistedDB.chatCommitQueries
      .selectByBranchId(conversationId, branchId.value, ::mapToCommit)
      .observeList()

    val peerReadAtFlow = peerReadAt(conversationId, selfId)

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
    firestore.branchUnreadCountLive(
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
  private fun peerReadAt(conversationId: String, selfId: UserId): Flow<LocalDateTime?> = flow {
    val peerId = withContext(Dispatchers.IO) {
      persistedDB.chatConversationMemberQueries
        .selectByConversation(conversationId, ::mapToMember)
        .executeAsList()
        .firstOrNull { it.id.value != selfId.value }
        ?.id
        ?.value
    }
    if (peerId != null) {
      firestore.memberLive(
        conversationId = conversationId,
        userId = UserId(peerId)
      ).map { member ->
        member?.lastReadAt
          ?.toEpochMillis()
          ?.toLocalDateTime()
      }.collect {
        emit(it)
      }
    } else {
      emit(null)
    }
  }

  private fun memberUids(conversationId: String): List<String> {
    return persistedDB.chatConversationMemberQueries
      .selectByConversation(conversationId, ::mapToMember)
      .executeAsList()
      .map { it.id.value }
  }

  private suspend fun applyBranchesChanges(
    conversationId: String,
    changes: List<FirestoreChange<BranchNM>>
  ) {
    return withContext(Dispatchers.IO) {
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

  private suspend fun applyUpdateUnreadCount(branchId: String, unreadCount: Long) {
    return withContext(Dispatchers.IO) {
      persistedDB.chatBranchQueries.updateUnreadCount(
        id = branchId,
        unreadCount = unreadCount
      )
    }
  }

  private suspend fun applyInsertOrReplaceCommits(
    conversationId: String,
    commits: List<CommitNM>
  ) {
    return withContext(Dispatchers.IO) {
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
  ) {
    return withContext(Dispatchers.IO) {
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

  private suspend fun awaitConversationId(): String {
    return persistedDB.chatBranchQueries
      .selectById(branchId.value, ::mapToBranch)
      .observeOneOrNull()
      .filterNotNull()
      .first()
      .conversationId
      .value
  }

  private fun requireConversationId(): String {
    return requireNotNull(
      persistedDB.chatBranchQueries
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
