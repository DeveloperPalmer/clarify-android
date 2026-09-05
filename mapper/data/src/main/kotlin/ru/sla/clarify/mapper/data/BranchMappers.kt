package ru.sla.clarify.mapper.data

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation

@Suppress("LongParameterList")
fun mapToBranch(
  id: Branch.Id,
  conversationId: Conversation.Id,
  parentBranchId: Branch.Id,
  branchedFromCommitId: Commit.Id,
  name: String,
  lastCommit: String?,
  lastCommitTimestamp: Long,
  unreadCount: Long,
  createdAt: Long,
  createdById: UserId,
  mergeRequestStatus: String?,
  mergeRequestInitiatorId: UserId?,
  mergeRequestRequestedAt: Long?,
  mergeRequestApprovedByIds: Set<UserId>?,
  mergeRequestMergedAt: Long?,
  mergeRequestMergedIntoBranchId: Branch.Id?
): Branch {
  return Branch(
    id = id,
    conversationId = conversationId,
    parentBranchId = parentBranchId,
    branchedFromCommitId = branchedFromCommitId,
    name = name,
    lastCommit = lastCommit,
    lastCommitAt = formatLastCommitTimestamp(lastCommitTimestamp),
    lastCommitTimestamp = lastCommitTimestamp,
    unreadCount = unreadCount,
    createdAt = createdAt,
    createdById = createdById,
    mergeRequest = mapToMergeRequest(
      status = mergeRequestStatus,
      initiatorId = mergeRequestInitiatorId,
      requestedAt = mergeRequestRequestedAt,
      approvedByIds = mergeRequestApprovedByIds,
      mergedAt = mergeRequestMergedAt,
      mergedIntoBranchId = mergeRequestMergedIntoBranchId
    )
  )
}
