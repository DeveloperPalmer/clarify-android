package ru.sla.clarify.feature.chat.thread.data

import com.squareup.anvil.annotations.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.database.InMemoryDB
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
import ru.sla.clarify.lib.google.firestore.entity.FirestoreBranch
import ru.sla.clarify.lib.google.firestore.entity.FirestoreCommit
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import javax.inject.Inject

@SingleIn(ThreadScope::class)
@ContributesBinding(ThreadScope::class)
class BranchRepositoryImpl @Inject constructor(
  private val firestore: Firestore,
  private val inMemoryDB: InMemoryDB,
  private val threadMediator: ThreadMediator
) : BranchRepository {

  override fun observeBranchChanges(): Flow<Unit> = flow {
    val conversationId = threadMediator.awaitConversationId()
    firestore.branchesLive(conversationId)
      .flowOn(Dispatchers.IO)
      .collect { changes ->
        handleBranchChanges(changes)
        emit(Unit)
      }
  }

  override fun branches(): Flow<List<Branch>> = flow {
    val conversationId = threadMediator.awaitConversationId()
    inMemoryDB.branchQueries
      .selectByConversationId(conversationId.value, ::mapToBranch)
      .observeList()
      .collect { emit(it) }
  }

  override fun branch(id: Branch.Id): Flow<Branch?> {
    return inMemoryDB.branchQueries
      .selectById(id.value, ::mapToBranch)
      .observeOneOrNull()
  }

  override suspend fun createBranch(
    parentId: Branch.Id?,
    branchedFrom: Commit.Id,
    name: String
  ): Branch = withContext(Dispatchers.IO) {
    val remote = firestore.postBranch(
      conversationId = threadMediator.requireConversationId(),
      parentBranchId = resolveBranchId(parentId),
      branchedFromCommitId = FirestoreCommit.Id(branchedFrom.value),
      name = name
    )
    val branch = remote.toDomain()
    insertOrReplace(branch)
    branch
  }

  override suspend fun openMergeRequest(branchId: Branch.Id) = withContext(Dispatchers.IO) {
    firestore.postMergeRequest(
      conversationId = threadMediator.requireConversationId(),
      branchId = FirestoreBranch.Id(branchId.value)
    )
  }

  override suspend fun approveMergeRequest(branchId: Branch.Id) = withContext(Dispatchers.IO) {
    firestore.patchMergeApproval(
      conversationId = threadMediator.requireConversationId(),
      branchId = FirestoreBranch.Id(branchId.value),
      participantUids = threadMediator.directParticipantIds()
    )
  }

  override suspend fun revokeApprovalMergeRequest(branchId: Branch.Id) = withContext(Dispatchers.IO) {
    firestore.deleteMergeApproval(
      conversationId = threadMediator.requireConversationId(),
      branchId = FirestoreBranch.Id(branchId.value)
    )
  }

  override suspend fun cancelMergeRequest(branchId: Branch.Id) = withContext(Dispatchers.IO) {
    firestore.deleteMergeRequest(
      conversationId = threadMediator.requireConversationId(),
      branchId = FirestoreBranch.Id(branchId.value)
    )
  }

  private fun handleBranchChanges(branches: List<FirestoreBranch>) {
    inMemoryDB.transaction {
      branches.forEach { branch ->
        when (branch.changeType) {
          FirestoreDocumentResult.Removed -> {
            inMemoryDB.branchQueries.deleteById(
              id = branch.id.value
            )
          }
          null,
          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            insertOrReplace(branch.toDomain())
          }
        }
      }
    }
  }

  private fun insertOrReplace(branch: Branch) {
    inMemoryDB.branchQueries.insertOrReplace(
      id = branch.id.value,
      conversationId = branch.conversationId.value,
      parentBranchId = branch.parentBranchId.value,
      branchedFromCommitId = branch.branchedFromCommitId.value,
      name = branch.name,
      status = branch.status.value,
      createdAt = branch.createdAt,
      createdByUid = branch.createdByUid.value,
      mergeRequestInitiatorUid = branch.mergeRequest?.initiatorUid?.value,
      mergeRequestRequestedAt = branch.mergeRequest?.requestedAt,
      mergeRequestApprovedByUids = branch.mergeRequest?.approvedByUids?.map { it.value },
      mergedAt = branch.mergedAt,
      mergedIntoBranchId = branch.mergedIntoBranchId?.value
    )
  }

  private suspend fun resolveBranchId(branchId: Branch.Id?): FirestoreBranch.Id {
    if (branchId != null) return FirestoreBranch.Id(branchId.value)
    val conversation = threadMediator.conversationId()
      ?: error("conversationId not found")
    return FirestoreBranch.Id(conversation.value)
  }
}
