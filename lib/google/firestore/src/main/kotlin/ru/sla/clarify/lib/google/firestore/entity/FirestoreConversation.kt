package ru.sla.clarify.lib.google.firestore.entity

data class FirestoreConversation(
  val id: String,
  val participantUids: List<String>,
  val lastCommitText: String?,
  val lastCommitAtEpochSeconds: Long,
  val changeType: FirestoreDocumentResult?
)
