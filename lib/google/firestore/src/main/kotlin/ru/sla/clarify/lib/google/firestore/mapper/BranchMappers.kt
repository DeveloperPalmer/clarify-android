package ru.sla.clarify.lib.google.firestore.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.BranchRecord
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.lib.google.firestore.entity.BranchNM
import ru.sla.clarify.lib.google.firestore.entity.MergeRequestNM
import ru.sla.clarify.lib.google.firestore.toEpochSeconds

internal fun BranchNM.toDomainModel(conversationId: Conversation.Id): BranchRecord {
  return BranchRecord(
    id = Branch.Id(id),
    conversationId = conversationId,
    parentBranchId = Branch.Id(parentBranchId),
    branchedFromCommitId = Commit.Id(branchedFromCommitId),
    name = name,
    lastCommitText = lastCommitText,
    lastCommitAtSeconds = lastCommitAt?.toEpochSeconds() ?: 0L,
    createdAtSeconds = createdAt?.toEpochSeconds() ?: 0L,
    createdById = UserId(createdByUid),
    mergeRequest = mergeRequest?.toDomainModel()
  )
}

internal fun MergeRequestNM.toDomainModel(): BranchRecord.MergeRequest {
  return BranchRecord.MergeRequest(
    status = Branch.MergeRequest.Status.fromValue(status.value),
    initiatorId = UserId(initiatorUid),
    requestedAtSeconds = requestedAt.toEpochSeconds(),
    approvedByIds = approvedByUids.map(::UserId).toSet(),
    mergedAtSeconds = mergedAt?.toEpochSeconds(),
    mergedIntoBranchId = mergedIntoBranchId?.let(Branch::Id)
  )
}
