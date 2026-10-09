package ru.sla.clarify.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.DistinctFlow
import ru.sla.clarify.database.entity.BranchRow
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
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

  /**
   * Не `INSERT OR REPLACE`: замена — это удаление и вставка, и внешний ключ каскадом удалил бы
   * запрос слияния ветки. `unreadCount` в обновление не входит — счётчик ведётся локально, и
   * повторная запись ветки с сервера его не сбрасывает.
   */
  @Query(
    """
    INSERT INTO ChatBranch (
      id,
      conversationId,
      parentBranchId,
      branchedFromCommitId,
      name,
      lastCommit,
      lastCommitTimestamp,
      createdAt,
      createdById
    )
    VALUES (
      :id,
      :conversationId,
      :parentBranchId,
      :branchedFromCommitId,
      :name,
      :lastCommit,
      :lastCommitTimestamp,
      :createdAt,
      :createdById
    )
    ON CONFLICT (id) DO UPDATE SET
      conversationId = excluded.conversationId,
      parentBranchId = excluded.parentBranchId,
      branchedFromCommitId = excluded.branchedFromCommitId,
      name = excluded.name,
      lastCommit = excluded.lastCommit,
      lastCommitTimestamp = excluded.lastCommitTimestamp,
      createdAt = excluded.createdAt,
      createdById = excluded.createdById
    """
  )
  suspend fun upsert(
    id: Branch.Id,
    conversationId: Conversation.Id,
    parentBranchId: Branch.Id,
    branchedFromCommitId: Commit.Id,
    name: String,
    lastCommit: String?,
    lastCommitTimestamp: Long,
    createdAt: Long,
    createdById: UserId
  )

  @Query(
    """
    UPDATE ChatBranch
    SET
      unreadCount = :unreadCount
    WHERE id = :id
    """
  )
  suspend fun updateUnreadCount(id: Branch.Id, unreadCount: Long)
}
