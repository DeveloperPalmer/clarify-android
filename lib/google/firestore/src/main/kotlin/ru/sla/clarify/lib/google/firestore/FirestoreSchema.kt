package ru.sla.clarify.lib.google.firestore

import com.google.firebase.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

object FirestoreSchema {
  const val USERS_COLLECTION = "users"
  const val CONVERSATIONS_COLLECTION = "conversations"
  const val COMMITS_COLLECTION = "commits"
  const val CONVERSATION_STATES_COLLECTION = "conversationStates"

  const val USER_DISPLAY_NAME = "displayName"
  const val USER_PHOTO_URL = "photoUrl"
  const val USER_CREATED_AT = "createdAt"
  const val USER_UPDATED_AT = "updatedAt"

  const val CONVERSATION_TYPE = "type"
  const val CONVERSATION_PARTICIPANT_UIDS = "participantUids"
  const val CONVERSATION_LAST_COMMIT_TEXT = "lastCommitText"
  const val CONVERSATION_LAST_COMMIT_SENDER_UID = "lastCommitSenderUid"
  const val CONVERSATION_LAST_COMMIT_AT = "lastCommitAt"
  const val CONVERSATION_UPDATED_AT = "updatedAt"

  const val COMMIT_CLIENT_COMMIT_ID = "clientCommitId"
  const val COMMIT_SENDER_UID = "senderUid"
  const val COMMIT_TEXT = "text"
  const val COMMIT_TYPE = "type"
  const val COMMIT_CREATED_AT = "createdAt"
  const val COMMIT_SERVER_CREATED_AT = "serverCreatedAt"
  const val COMMIT_READ_BY = "readBy"
  const val COMMIT_COLOR_HEX = "colorHex"

  const val STATE_LAST_READ_AT = "lastReadAt"
  const val STATE_UNREAD_COUNT = "unreadCount"

  enum class ConversationType(val value: String) {
    Direct("direct"),
    Group("group");

    companion object {
      fun fromValue(value: String): ConversationType? {
        return entries.firstOrNull { it.value == value }
      }
    }
  }

  enum class CommitType(val value: String) {
    Text("text")
  }
}

fun Timestamp.toEpochSeconds(): Long {
  return seconds
}

fun LocalDateTime.toTimestamp(): Timestamp {
  val instant = atZone(ZoneId.systemDefault())
    .toInstant()
  return Timestamp(Date.from(instant))
}
