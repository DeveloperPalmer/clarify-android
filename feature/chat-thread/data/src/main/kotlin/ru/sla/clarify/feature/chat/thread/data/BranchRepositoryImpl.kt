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
    firestore.observeBranches(conversationId)
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
    parentBranchId: Branch.Id,
    branchedFromCommitId: Commit.Id,
    name: String
  ): Branch = withContext(Dispatchers.IO) {
    val remote = firestore.createBranch(
      conversationId = threadMediator.requireConversationId(),
      parentBranchId = FirestoreBranch.Id(parentBranchId.value),
      branchedFromCommitId = FirestoreCommit.Id(branchedFromCommitId.value),
      name = name
    )
    val branch = remote.toDomain()
    inMemoryDB.branchQueries.insertOrReplace(
      id = branch.id.value,
      conversationId = branch.conversationId.value,
      parentBranchId = branch.parentBranchId.value,
      branchedFromCommitId = branch.branchedFromCommitId.value,
      name = branch.name,
      status = branch.status.value,
      createdAt = branch.createdAt,
      createdByUid = branch.createdByUid.value
    )
    branch
  }

  private fun handleBranchChanges(branches: List<FirestoreBranch>) {
    inMemoryDB.transaction {
      branches.forEach { branch ->
        when (branch.changeType) {
          FirestoreDocumentResult.Removed -> {
            inMemoryDB.branchQueries.delete(
              id = branch.id.value
            )
          }
          null,
          FirestoreDocumentResult.Added,
          FirestoreDocumentResult.Modified -> {
            inMemoryDB.branchQueries.insertOrReplace(
              id = branch.id.value,
              conversationId = branch.conversationId.value,
              parentBranchId = branch.parentBranchId.value,
              branchedFromCommitId = branch.branchedFromCommitId.value,
              name = branch.name,
              status = branch.status.value,
              createdAt = branch.createdAtEpochSeconds,
              createdByUid = branch.createdByUid.value
            )
          }
        }
      }
    }
  }
}
