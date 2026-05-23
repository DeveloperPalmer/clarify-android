package ru.sla.clarify.lib.google.firestore.entity

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BranchStatus

data class FirestoreBranch(
  val id: Id,
  val conversationId: FirestoreConversation.Id,
  val parentBranchId: Id,
  val branchedFromCommitId: FirestoreCommit.Id,
  val name: String,
  val status: BranchStatus,
  val createdAtEpochSeconds: Long,
  val createdByUid: UserId,
  val mergeRequest: MergeRequest?,
  val mergedAtEpochSeconds: Long?,
  val mergedIntoBranchId: Id?,
  val changeType: FirestoreDocumentResult?
) {
  @JvmInline
  value class Id(val value: String)

  data class MergeRequest(
    val initiatorUid: UserId,
    val requestedAtEpochSeconds: Long,
    val approvedByUids: List<UserId>
  )
}
