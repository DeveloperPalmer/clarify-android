package ru.sla.clarify.mapper.data

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.toEpochSeconds

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

fun BranchNM.toDomain(conversationId: Conversation.Id): Branch {
  val createdAt = createdAt?.toEpochSeconds() ?: 0L
  val lastCommitAt = lastCommitAt?.toEpochSeconds()
  return Branch(
    id = Branch.Id(id),
    conversationId = conversationId,
    parentBranchId = Branch.Id(parentBranchId),
    branchedFromCommitId = Commit.Id(branchedFromCommitId),
    name = name,
    lastCommit = lastCommitText,
    lastCommitAt = formatLastCommitTimestamp(lastCommitAt),
    lastCommitTimestamp = lastCommitAt ?: 0L,
    unreadCount = 0L,
    createdAt = createdAt,
    createdById = UserId(createdByUid),
    mergeRequest = mergeRequest?.let { mr ->
      Branch.MergeRequest(
        status = Branch.MergeRequest.Status.fromValue(mr.status.value),
        initiatorId = UserId(mr.initiatorUid),
        requestedAt = mr.requestedAt.toEpochSeconds(),
        approvedByIds = mr.approvedByUids.map(::UserId).toSet(),
        mergedAt = mr.mergedAt?.toEpochSeconds(),
        mergedIntoBranchId = mr.mergedIntoBranchId?.let(Branch::Id)
      )
    }
  )
}
