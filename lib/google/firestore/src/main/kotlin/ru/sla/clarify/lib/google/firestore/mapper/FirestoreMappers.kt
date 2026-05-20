package ru.sla.clarify.lib.google.firestore.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import ru.sla.clarify.auth.session.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_COLOR_HEX
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_CREATED_AT
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_SENDER_UID
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_TEXT
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATION_LAST_COMMIT_AT
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATION_LAST_COMMIT_TEXT
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATION_PARTICIPANT_UIDS
import ru.sla.clarify.lib.google.firestore.entity.FirestoreCommit
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.toEpochSeconds

fun extractConversationFB(
  document: DocumentSnapshot,
  type: DocumentChange.Type? = null
): FirestoreConversation {
  return FirestoreConversation(
    id = document.id,
    changeType = type
      ?.toDomainModel(),
    participantUids = document
      .stringList(CONVERSATION_PARTICIPANT_UIDS),
    lastCommitText = document
      .getString(CONVERSATION_LAST_COMMIT_TEXT),
    lastCommitAtEpochSeconds = document
      .getTimestamp(CONVERSATION_LAST_COMMIT_AT)
      ?.toEpochSeconds()
      ?: 0L
  )
}

internal fun extractCommitFB(
  document: DocumentSnapshot,
  type: DocumentChange.Type? = null
): FirestoreCommit {
  val createdAt = document
    .getTimestamp(COMMIT_CREATED_AT)
    ?: Timestamp.now()
  val senderId = document.requireField(COMMIT_SENDER_UID)
  return FirestoreCommit(
    commitId = FirestoreCommit.Id(document.id),
    senderId = UserId(senderId),
    changeType = type
      ?.toDomainModel(),
    text = document
      .field(COMMIT_TEXT)
      .orEmpty(),
    colorHex = document
      .requireField(COMMIT_COLOR_HEX),
    createdAtEpochSeconds = createdAt
      .toEpochSeconds()
  )
}

private fun DocumentChange.Type.toDomainModel(): FirestoreDocumentResult {
  return when (this) {
    DocumentChange.Type.ADDED -> FirestoreDocumentResult.Added
    DocumentChange.Type.MODIFIED -> FirestoreDocumentResult.Modified
    DocumentChange.Type.REMOVED -> FirestoreDocumentResult.Removed
  }
}

private fun DocumentSnapshot.requireField(key: String): String {
  return requireNotNull(getString(key))
}

private fun DocumentSnapshot.field(key: String): String? {
  return getString(key)?.takeIf { it.isNotBlank() }
}

private fun DocumentSnapshot.stringList(field: String): List<String> {
  return get(field)
    ?.let { it as? List<*> }
    ?.mapNotNull { it as? String }
    .orEmpty()
}
