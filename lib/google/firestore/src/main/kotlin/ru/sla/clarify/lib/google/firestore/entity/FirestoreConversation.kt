package ru.sla.clarify.lib.google.firestore.entity

import ru.sla.clarify.lib.google.firestore.FirestoreSchema.ConversationType

data class FirestoreConversation(
  val id: String,
  val type: ConversationType?,
  val participantUids: List<String>,
  val lastCommitText: String?,
  val lastCommitAtEpochSeconds: Long,
  val changeType: FirestoreDocumentResult?
)
