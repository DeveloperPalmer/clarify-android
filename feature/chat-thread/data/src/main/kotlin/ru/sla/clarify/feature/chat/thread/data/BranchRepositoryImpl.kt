package ru.sla.clarify.feature.chat.thread.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

  override fun observeBranchChanges(): Flow<Unit> = flow {
    val conversationId = threadMediator.awaitConversationId()
    firestore.branchesLive(conversationId)
      .flowOn(Dispatchers.IO)
      .collect { changes ->
        handleBranchChanges(conversationId, changes)
        emit(Unit)
      }
  }

  override fun branches(): Flow<List<Branch>> = flow {
    val conversationId = threadMediator.awaitConversationId()
    persistedDB.branchQueries
      .selectByConversationId(conversationId, ::mapToBranch)
      .observeList()
      .collect { emit(it) }
  }

  override fun branch(id: Branch.Id): Flow<Branch?> {
    return persistedDB.branchQueries
      .selectById(id.value, ::mapToBranch)
      .observeOneOrNull()
  }

  override suspend fun createBranch(
    parentId: Branch.Id?,
    branchedFrom: Commit.Id,
    name: String
  ): Branch = withContext(Dispatchers.IO) {
    val conversationId = threadMediator.requireConversationId()
    val remote = firestore.postBranch(
      conversationId = conversationId,
      parentBranchId = resolveBranchId(parentId),
      branchedFromCommitId = branchedFrom.value,
      name = name
    )
    val branch = remote.toDomain(conversationId)
    insertOrReplace(branch)
    branch
  }

  override suspend fun openMergeRequest(branchId: Branch.Id) = withContext(Dispatchers.IO) {
    firestore.postMergeRequest(
      conversationId = threadMediator.requireConversationId(),
      branchId = branchId.value
    )
  }

  override suspend fun approveMergeRequest(branchId: Branch.Id) = withContext(Dispatchers.IO) {
    firestore.patchMergeApproval(
      conversationId = threadMediator.requireConversationId(),
      branchId = branchId.value,
      participantUids = threadMediator.directParticipantIds()
    )
  }

  override suspend fun revokeApprovalMergeRequest(branchId: Branch.Id) = withContext(Dispatchers.IO) {
    firestore.deleteMergeApproval(
      conversationId = threadMediator.requireConversationId(),
      branchId = branchId.value
    )
  }

  override suspend fun cancelMergeRequest(branchId: Branch.Id) = withContext(Dispatchers.IO) {
    firestore.deleteMergeRequest(
      conversationId = threadMediator.requireConversationId(),
      branchId = branchId.value
    )
  }

  override suspend fun finalizeMergeRequest(branchId: Branch.Id) = withContext(Dispatchers.IO) {
    firestore.patchMergeFinalize(
      conversationId = threadMediator.requireConversationId(),
      branchId = branchId.value
    )
  }

  private fun handleBranchChanges(
    conversationId: String,
    changes: List<FirestoreChange<BranchNM>>
  ) {
    persistedDB.transaction {
      changes.forEach { change ->
        val branch = change.data
        when (change.changeType) {
          FirestoreDocumentResult.Removed -> {
            persistedDB.branchQueries.deleteById(id = branch.id)
          }
          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            insertOrReplace(branch.toDomain(conversationId))
          }
        }
      }
    }
  }

  private fun insertOrReplace(branch: Branch) {
    persistedDB.branchQueries.insertOrReplace(
      id = branch.id.value,
      conversationId = branch.conversationId.value,
      parentBranchId = branch.parentBranchId.value,
      branchedFromCommitId = branch.branchedFromCommitId.value,
      name = branch.name,
      createdAt = branch.createdAt,
      createdByUid = branch.createdByUid.value
    )
    val mergeRequest = branch.mergeRequest
    if (mergeRequest != null) {
      persistedDB.mergeRequestQueries.insertOrReplace(
        branchId = branch.id.value,
        status = mergeRequest.status.value,
        initiatorUid = mergeRequest.initiatorUid.value,
        requestedAt = mergeRequest.requestedAt,
        approvedByUids = mergeRequest.approvedByUids.map { it.value },
        mergedAt = mergeRequest.mergedAt,
        mergedIntoBranchId = mergeRequest.mergedIntoBranchId?.value
      )
    } else {
      persistedDB.mergeRequestQueries.deleteByBranchId(branch.id.value)
    }
  }

  private suspend fun resolveBranchId(branchId: Branch.Id?): String {
    if (branchId != null) return branchId.value
    val conversation = threadMediator.conversationId()
      ?: error("conversationId not found")
    return conversation
  }
}
