package ru.sla.clarify.feature.chat.thread.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.database.PersistedDB
import ru.sla.clarify.database.extension.observeList
import ru.sla.clarify.database.extension.observeOneOrNull
import ru.sla.clarify.feature.chat.thread.data.common.ThreadMediator
import ru.sla.clarify.feature.chat.thread.data.mapper.mapToBranch
import ru.sla.clarify.feature.chat.thread.data.mapper.toDomain
import ru.sla.clarify.feature.chat.thread.domain.BranchRepository
import ru.sla.clarify.feature.chat.thread.domain.di.ThreadScope
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.lib.google.firestore.Firestore
import ru.sla.clarify.lib.google.firestore.FirestoreChange
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import javax.inject.Inject

@SingleIn(ThreadScope::class)
@ContributesBinding(ThreadScope::class)
class BranchRepositoryImpl @Inject constructor(
  private val firestore: Firestore,
  private val persistedDB: PersistedDB,
  private val threadMediator: ThreadMediator
) : BranchRepository {

  override suspend fun subscribeOnBranchChanges() {
    val conversationId = threadMediator.awaitConversationId()
    firestore.branchesLive(conversationId)
      .flowOn(Dispatchers.IO)
      .collect(::applyBranchesChanges)
  }

  override suspend fun subscribeOnBranchesUnreadCounts() {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.branchQueries
      .selectIdsByConversationId(conversationId)
      .observeList()
      .flowOn(Dispatchers.IO)
      .collectLatest(::subscribeOnBranchesUnreadCount)
  }

  private suspend fun subscribeOnBranchesUnreadCount(ids: List<String>) = coroutineScope {
    val conversationId = threadMediator.awaitConversationId()
    ids.forEach { branchId ->
      launch {
        firestore.branchUnreadCountLive(
          conversationId = conversationId,
          branchId = branchId
        ).collect { unreadCount ->
          applyUpdateUnreadCount(
            branchId = branchId,
            unreadCount = unreadCount
          )
        }
      }
    }
  }

  override suspend fun markAsRead(branchId: Branch.Id) {
    return withContext(Dispatchers.IO) {
      firestore.patchBranchClearUnreadCount(
        conversationId = threadMediator.requireConversationId(),
        branchId = branchId.value
      )
    }
  }

  override suspend fun createBranch(parentId: Branch.Id?, from: Commit.Id, name: String): Branch {
    return withContext(Dispatchers.IO) {
      val conversationId = threadMediator.requireConversationId()
      val remote = firestore.postBranch(
        conversationId = conversationId,
        parentBranchId = resolveBranchId(parentId),
        branchedFromCommitId = from.value,
        name = name
      )
      val branch = remote.toDomain(conversationId)
      applyInsertOrReplace(branch)
      branch
    }
  }

  override suspend fun openMergeRequest(branchId: Branch.Id) {
    return withContext(Dispatchers.IO) {
      firestore.postMergeRequest(
        conversationId = threadMediator.requireConversationId(),
        branchId = branchId.value
      )
    }
  }

  override suspend fun approveMergeRequest(branchId: Branch.Id) {
    return withContext(Dispatchers.IO) {
      firestore.patchMergeApproval(
        conversationId = threadMediator.requireConversationId(),
        branchId = branchId.value,
        participantUids = threadMediator.directParticipantIds()
      )
    }
  }

  override suspend fun revokeApprovalMergeRequest(branchId: Branch.Id) {
    return withContext(Dispatchers.IO) {
      firestore.deleteMergeApproval(
        conversationId = threadMediator.requireConversationId(),
        branchId = branchId.value
      )
    }
  }

  override suspend fun cancelMergeRequest(branchId: Branch.Id) {
    return withContext(Dispatchers.IO) {
      firestore.deleteMergeRequest(
        conversationId = threadMediator.requireConversationId(),
        branchId = branchId.value
      )
    }
  }

  override suspend fun finalizeMergeRequest(branchId: Branch.Id) {
    return withContext(Dispatchers.IO) {
      firestore.patchMergeFinalize(
        conversationId = threadMediator.requireConversationId(),
        branchId = branchId.value
      )
    }
  }

  override val branches: Flow<List<Branch>> = flow {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.branchQueries
      .selectByConversationId(conversationId, ::mapToBranch)
      .observeList()
      .flowOn(Dispatchers.IO)
      .collect { emit(it) }
  }

  override fun branch(id: Branch.Id): Flow<Branch?> {
    return persistedDB.branchQueries
      .selectById(id.value, ::mapToBranch)
      .observeOneOrNull()
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
            applyInsertOrReplace(change.data.toDomain(conversationId))
          }
        }
      }
    }
  }

  private fun applyInsertOrReplace(branch: Branch) {
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

  private suspend fun resolveBranchId(branchId: Branch.Id?): String {
    if (branchId != null) return branchId.value
    val conversation = threadMediator.conversationId() ?: error("conversationId not found")
    return conversation
  }
}
