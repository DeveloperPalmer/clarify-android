package ru.sla.clarify.lib.google.firestore.entity

import ru.sla.clarify.auth.session.domain.entity.UserId

data class FirestoreCommit(
  val commitId: Id,
  val parentCommitId: Id?,
  val senderId: UserId,
  val text: String,
  val colorHex: String,
  val createdAtEpochSeconds: Long,
  val changeType: FirestoreDocumentResult?
) {
  @JvmInline
  value class Id(val value: String)
}
