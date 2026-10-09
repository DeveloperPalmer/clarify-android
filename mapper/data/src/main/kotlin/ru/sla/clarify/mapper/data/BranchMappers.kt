package ru.sla.clarify.mapper.data

import ru.sla.clarify.database.entity.BranchRow
import ru.sla.clarify.entity.chat.Branch

fun BranchRow.toDomainModel(): Branch {
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
