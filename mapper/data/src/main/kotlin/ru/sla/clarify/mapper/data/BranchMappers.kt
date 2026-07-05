package ru.sla.clarify.mapper.data

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.adapter.StringList
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.toEpochSeconds

@Suppress("LongParameterList")
fun mapToBranch(
  id: String,
  conversationId: String,
  parentBranchId: String,
  branchedFromCommitId: String,
  name: String,
  lastCommit: String?,
  lastCommitTimestamp: Long,
  unreadCount: Long,
  createdAt: Long,
  createdByUid: String,
  mergeRequestStatus: String?,
  mergeRequestInitiatorUid: String?,
  mergeRequestRequestedAt: Long?,
  mergeRequestApprovedByUids: StringList?,
  mergeRequestMergedAt: Long?,
  mergeRequestMergedIntoBranchId: String?
): Branch {
  return Branch(
    id = Branch.Id(id),
    conversationId = Conversation.Id(conversationId),
    parentBranchId = Branch.Id(parentBranchId),
    branchedFromCommitId = Commit.Id(branchedFromCommitId),
    name = name,
    lastCommit = lastCommit,
    lastCommitAt = formatLastCommitTimestamp(lastCommitTimestamp),
    lastCommitTimestamp = lastCommitTimestamp,
    unreadCount = unreadCount,
    createdAt = createdAt,
    createdById = UserId(createdByUid),
    mergeRequest = mapToMergeRequest(
      status = mergeRequestStatus,
      initiatorUid = mergeRequestInitiatorUid,
      requestedAt = mergeRequestRequestedAt,
      approvedByUids = mergeRequestApprovedByUids,
      mergedAt = mergeRequestMergedAt,
      mergedIntoBranchId = mergeRequestMergedIntoBranchId
    )
  )
}

fun BranchNM.toDomain(conversationId: String): Branch {
  val createdAt = createdAt?.toEpochSeconds() ?: 0L
  val lastCommitAt = lastCommitAt?.toEpochSeconds()
  return Branch(
    id = Branch.Id(id),
    conversationId = Conversation.Id(conversationId),
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
