package ru.sla.clarify.lib.google.firestore

import com.google.firebase.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

object FirestoreSchema {
  const val USERS_COLLECTION = "users"
  const val PARTICIPANTS_COLLECTION = "participants"
  const val CONVERSATIONS_COLLECTION = "conversations"
  const val COMMITS_COLLECTION = "commits"
  const val UNREAD_COMMITS_COLLECTION = "unreadCommits"
  const val BRANCHES_COLLECTION = "branches"

  const val UNREAD_COMMITS_COUNT = "count"

  const val USER_DISPLAY_NAME = "displayName"
  const val USER_PHOTO_URL = "photoUrl"
  const val USER_CREATED_AT = "createdAt"
  const val USER_UPDATED_AT = "updatedAt"

  const val PARTICIPANT_ID = "id"
  const val PARTICIPANT_DISPLAY_NAME = "displayName"
  const val PARTICIPANT_PHOTO_URL = "photoUrl"

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
  const val COMMIT_BRANCH_ID = "branchId"

  const val BRANCH_PARENT_ID = "parentBranchId"
  const val BRANCH_BRANCHED_FROM_COMMIT_ID = "branchedFromCommitId"
  const val BRANCH_NAME = "name"
  const val BRANCH_STATUS = "status"
  const val BRANCH_CREATED_AT = "createdAt"
  const val BRANCH_CREATED_BY_UID = "createdByUid"
  const val BRANCH_MERGE_REQUEST = "mergeRequest"
  const val BRANCH_MERGE_REQUEST_INITIATOR_UID = "initiatorUid"
  const val BRANCH_MERGE_REQUEST_REQUESTED_AT = "requestedAt"
  const val BRANCH_MERGE_REQUEST_APPROVED_BY_UIDS = "approvedByUids"
  const val BRANCH_MERGED_AT = "mergedAt"
  const val BRANCH_MERGED_INTO_BRANCH_ID = "mergedIntoBranchId"

  enum class ConversationType(val value: String) {
    Direct("direct"),
    Group("group");

    companion object {
      fun fromValue(value: String): ConversationType {
        return entries.first { it.value == value }
      }
    }
  }

  enum class CommitType(val value: String) {
    Text("text")
  }

  enum class BranchStatus(val value: String) {
    Active("active"),
    MergeInProgress("mergeInProgress"),
    Merged("merged");

    companion object {
      fun fromValue(value: String): BranchStatus {
        return entries.firstOrNull { it.value == value }
          ?: error("unexpected branch status: $value")
      }
    }
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
