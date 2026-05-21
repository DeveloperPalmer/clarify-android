package ru.sla.clarify.feature.chat.thread.data.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.lib.google.firestore.entity.FirestoreBranch

internal fun mapToBranch(
  id: String,
  conversationId: String,
  parentBranchId: String,
  branchedFromCommitId: String,
  name: String,
  status: String,
  createdAt: Long,
  createdByUid: String
): Branch {
  return Branch(
    id = Branch.Id(id),
    conversationId = Conversation.Id(conversationId),
    parentBranchId = Branch.Id(parentBranchId),
    branchedFromCommitId = Commit.Id(branchedFromCommitId),
    name = name,
    status = Branch.Status.fromValue(status),
    createdAt = createdAt,
    createdByUid = UserId(createdByUid)
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
    createdByUid = createdByUid
  )
}
