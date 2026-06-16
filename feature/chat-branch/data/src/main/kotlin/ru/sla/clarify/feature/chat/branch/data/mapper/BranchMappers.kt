package ru.sla.clarify.feature.chat.branch.data.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.adapter.StringList
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.toEpochSeconds
import ru.sla.clarify.mapper.formatLastCommitTimestamp
import ru.sla.clarify.mapper.mapToMergeRequest

@Suppress("LongParameterList")
internal fun mapToBranch(
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

internal fun BranchNM.toDomain(conversationId: String): Branch {
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
    mergeRequest = mergeRequest?.let {
      Branch.MergeRequest(
        status = Branch.MergeRequest.Status.fromValue(it.status.value),
        initiatorId = UserId(it.initiatorUid),
        requestedAt = it.requestedAt.toEpochSeconds(),
        approvedByIds = it.approvedByUids.map(::UserId).toSet(),
        mergedAt = it.mergedAt?.toEpochSeconds(),
        mergedIntoBranchId = it.mergedIntoBranchId?.let(Branch::Id)
      )
    }
  )
}
