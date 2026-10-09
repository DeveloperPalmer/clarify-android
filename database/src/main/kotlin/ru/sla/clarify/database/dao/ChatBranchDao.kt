package ru.sla.clarify.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import ru.sla.clarify.database.DistinctFlow
import ru.sla.clarify.database.entity.BranchRow
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Conversation

@Dao
interface ChatBranchDao {

  @Query(
    """
    SELECT
      id
    FROM ChatBranch
    WHERE conversationId = :conversationId
    """
  )
  fun observeIds(conversationId: Conversation.Id): DistinctFlow<List<Branch.Id>>

  @Query(
    """
    SELECT
      ChatBranch.id,
      ChatBranch.conversationId,
      ChatBranch.parentBranchId,
      ChatBranch.branchedFromCommitId,
      ChatBranch.name,
      ChatBranch.lastCommit,
      ChatBranch.lastCommitTimestamp,
      ChatBranch.unreadCount,
      ChatBranch.createdAt,
      ChatBranch.createdById,
      MergeRequest.status AS mergeRequestStatus,
      MergeRequest.initiatorId AS mergeRequestInitiatorId,
      MergeRequest.requestedAt AS mergeRequestRequestedAt,
      MergeRequest.approvedByIds AS mergeRequestApprovedByIds,
      MergeRequest.mergedAt AS mergeRequestMergedAt,
      MergeRequest.mergedIntoBranchId AS mergeRequestMergedIntoBranchId
    FROM ChatBranch
    LEFT JOIN MergeRequest
      ON MergeRequest.branchId = ChatBranch.id
    WHERE ChatBranch.conversationId = :conversationId
    ORDER BY ChatBranch.createdAt ASC
    """
  )
  fun observeByConversation(conversationId: Conversation.Id): DistinctFlow<List<BranchRow>>

  @Query(
    """
    SELECT
      ChatBranch.id,
      ChatBranch.conversationId,
      ChatBranch.parentBranchId,
      ChatBranch.branchedFromCommitId,
      ChatBranch.name,
      ChatBranch.lastCommit,
      ChatBranch.lastCommitTimestamp,
      ChatBranch.unreadCount,
      ChatBranch.createdAt,
      ChatBranch.createdById,
      MergeRequest.status AS mergeRequestStatus,
      MergeRequest.initiatorId AS mergeRequestInitiatorId,
      MergeRequest.requestedAt AS mergeRequestRequestedAt,
      MergeRequest.approvedByIds AS mergeRequestApprovedByIds,
      MergeRequest.mergedAt AS mergeRequestMergedAt,
      MergeRequest.mergedIntoBranchId AS mergeRequestMergedIntoBranchId
    FROM ChatBranch
    LEFT JOIN MergeRequest
      ON MergeRequest.branchId = ChatBranch.id
    WHERE ChatBranch.id = :id
    """
  )
  fun observeById(id: Branch.Id): DistinctFlow<BranchRow?>

  @Query(
    """
    SELECT
      ChatBranch.id,
      ChatBranch.conversationId,
      ChatBranch.parentBranchId,
      ChatBranch.branchedFromCommitId,
      ChatBranch.name,
      ChatBranch.lastCommit,
      ChatBranch.lastCommitTimestamp,
      ChatBranch.unreadCount,
      ChatBranch.createdAt,
      ChatBranch.createdById,
      MergeRequest.status AS mergeRequestStatus,
      MergeRequest.initiatorId AS mergeRequestInitiatorId,
      MergeRequest.requestedAt AS mergeRequestRequestedAt,
      MergeRequest.approvedByIds AS mergeRequestApprovedByIds,
      MergeRequest.mergedAt AS mergeRequestMergedAt,
      MergeRequest.mergedIntoBranchId AS mergeRequestMergedIntoBranchId
    FROM ChatBranch
    LEFT JOIN MergeRequest
      ON MergeRequest.branchId = ChatBranch.id
    WHERE ChatBranch.id = :id
    """
  )
  suspend fun selectById(id: Branch.Id): BranchRow?
}
