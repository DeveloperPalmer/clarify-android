package ru.sla.clarify.feature.chat.thread.data.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.adapter.StringList
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.lib.google.firestore.entity.FirestoreBranch

@Suppress("LongParameterList")
internal fun mapToBranch(
  id: String,
  conversationId: String,
  parentBranchId: String,
  branchedFromCommitId: String,
  name: String,
  status: String,
  createdAt: Long,
  createdByUid: String,
  mergeRequestInitiatorUid: String?,
  mergeRequestRequestedAt: Long?,
  mergeRequestApprovedByUids: StringList?,
  mergedAt: Long?,
  mergedIntoBranchId: String?
): Branch {
  return Branch(
    id = Branch.Id(id),
    conversationId = Conversation.Id(conversationId),
    parentBranchId = Branch.Id(parentBranchId),
    branchedFromCommitId = Commit.Id(branchedFromCommitId),
    name = name,
    status = Branch.Status.fromValue(status),
    createdAt = createdAt,
    createdByUid = UserId(createdByUid),
    mergeRequest = buildMergeRequest(
      initiatorUid = mergeRequestInitiatorUid,
      requestedAt = mergeRequestRequestedAt,
      approvedByUids = mergeRequestApprovedByUids
    ),
    mergedAt = mergedAt,
    mergedIntoBranchId = mergedIntoBranchId?.let(Branch::Id)
  )
}

internal fun FirestoreBranch.toDomain(): Branch {
  return Branch(
    id = Branch.Id(id.value),
    conversationId = Conversation.Id(conversationId.value),
    parentBranchId = Branch.Id(parentBranchId.value),
    branchedFromCommitId = Commit.Id(branchedFromCommitId.value),
    name = name,
    status = Branch.Status.fromValue(status.value),
    createdAt = createdAtEpochSeconds,
    createdByUid = createdByUid,
    mergeRequest = mergeRequest?.let {
      Branch.MergeRequest(
        initiatorUid = it.initiatorUid,
        requestedAt = it.requestedAtEpochSeconds,
        approvedByUids = it.approvedByUids.toSet()
      )
    },
    mergedAt = mergedAtEpochSeconds,
    mergedIntoBranchId = mergedIntoBranchId?.let { Branch.Id(it.value) }
  )
}

private fun buildMergeRequest(
  initiatorUid: String?,
  requestedAt: Long?,
  approvedByUids: StringList?
): Branch.MergeRequest? {
  if (initiatorUid == null || requestedAt == null) return null
  return Branch.MergeRequest(
    initiatorUid = UserId(initiatorUid),
    requestedAt = requestedAt,
    approvedByUids = approvedByUids.orEmpty().map(::UserId).toSet()
  )
}
