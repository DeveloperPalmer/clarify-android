package ru.sla.clarify.feature.chat.thread.data.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.adapter.StringList
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.toEpochSeconds

@Suppress("LongParameterList")
internal fun mapToBranch(
  id: String,
  conversationId: String,
  parentBranchId: String,
  branchedFromCommitId: String,
  name: String,
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
    createdAt = createdAt,
    createdByUid = UserId(createdByUid),
    mergeRequest = buildMergeRequest(
      status = mergeRequestStatus,
      initiatorUid = mergeRequestInitiatorUid,
      requestedAt = mergeRequestRequestedAt,
      approvedByUids = mergeRequestApprovedByUids,
      mergedAt = mergeRequestMergedAt,
      mergedIntoBranchId = mergeRequestMergedIntoBranchId
    )
  )
}

internal fun BranchNM.toDomain(conversationId: String): Branch {
  return Branch(
    id = Branch.Id(id),
    conversationId = Conversation.Id(conversationId),
    parentBranchId = Branch.Id(parentBranchId),
    branchedFromCommitId = Commit.Id(branchedFromCommitId),
    name = name,
    createdAt = createdAt?.toEpochSeconds() ?: 0L,
    createdByUid = UserId(createdByUid),
    mergeRequest = mergeRequest?.let { mr ->
      Branch.MergeRequest(
        status = Branch.MergeRequest.Status.fromValue(mr.status.value),
        initiatorUid = UserId(mr.initiatorUid),
        requestedAt = mr.requestedAt.toEpochSeconds(),
        approvedByUids = mr.approvedByUids.map(::UserId).toSet(),
        mergedAt = mr.mergedAt?.toEpochSeconds(),
        mergedIntoBranchId = mr.mergedIntoBranchId?.let(Branch::Id)
      )
    }
  )
}

@Suppress("LongParameterList")
private fun buildMergeRequest(
  status: String?,
  initiatorUid: String?,
  requestedAt: Long?,
  approvedByUids: StringList?,
  mergedAt: Long?,
  mergedIntoBranchId: String?
): Branch.MergeRequest? {
  if (status == null || initiatorUid == null || requestedAt == null) return null
  return Branch.MergeRequest(
    status = Branch.MergeRequest.Status.fromValue(status),
    initiatorUid = UserId(initiatorUid),
    requestedAt = requestedAt,
    approvedByUids = approvedByUids.orEmpty().map(::UserId).toSet(),
    mergedAt = mergedAt,
    mergedIntoBranchId = mergedIntoBranchId?.let(Branch::Id)
  )
}
