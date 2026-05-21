package ru.sla.clarify.lib.google.firestore.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BRANCH_BRANCHED_FROM_COMMIT_ID
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BRANCH_CREATED_AT
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BRANCH_CREATED_BY_UID
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BRANCH_NAME
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BRANCH_PARENT_ID
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BRANCH_STATUS
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BranchStatus
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_BRANCH_ID
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_COLOR_HEX
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_CREATED_AT
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_SENDER_UID
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.COMMIT_TEXT
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATION_LAST_COMMIT_AT
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATION_LAST_COMMIT_TEXT
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATION_PARTICIPANT_UIDS
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.CONVERSATION_TYPE
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.ConversationType
import ru.sla.clarify.lib.google.firestore.entity.FirestoreBranch
import ru.sla.clarify.lib.google.firestore.entity.FirestoreCommit
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation
import ru.sla.clarify.lib.google.firestore.entity.FirestoreDocumentResult
import ru.sla.clarify.lib.google.firestore.toEpochSeconds

fun extractConversationFB(
  document: DocumentSnapshot,
  type: DocumentChange.Type? = null
): FirestoreConversation {
  return FirestoreConversation(
    id = FirestoreConversation.Id(document.id),
    type = ConversationType.fromValue(document.requireField(CONVERSATION_TYPE)),
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
  conversationId: FirestoreConversation.Id,
  type: DocumentChange.Type? = null
): FirestoreCommit {
  val createdAt = document
    .getTimestamp(COMMIT_CREATED_AT)
    ?: Timestamp.now()
  val senderId = document.requireField(COMMIT_SENDER_UID)
  return FirestoreCommit(
    commitId = FirestoreCommit.Id(document.id),
    conversationId = conversationId,
    branchId = FirestoreBranch.Id(document.requireField(COMMIT_BRANCH_ID)),
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

internal fun extractBranchFB(
  document: DocumentSnapshot,
  conversationId: FirestoreConversation.Id,
  type: DocumentChange.Type? = null
): FirestoreBranch {
  val parentBranchId = document.requireField(BRANCH_PARENT_ID)
  val branchedFromCommitId = document.requireField(BRANCH_BRANCHED_FROM_COMMIT_ID)

  val createdAt = document.getTimestamp(BRANCH_CREATED_AT) ?: Timestamp.now()

  return FirestoreBranch(
    id = FirestoreBranch.Id(document.id),
    conversationId = conversationId,
    parentBranchId = FirestoreBranch.Id(parentBranchId),
    branchedFromCommitId = FirestoreCommit.Id(branchedFromCommitId),
    name = document.requireField(BRANCH_NAME),
    status = BranchStatus.fromValue(document.requireField(BRANCH_STATUS)),
    createdAtEpochSeconds = createdAt.toEpochSeconds(),
    createdByUid = UserId(document.requireField(BRANCH_CREATED_BY_UID)),
    changeType = type?.toDomainModel()
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
