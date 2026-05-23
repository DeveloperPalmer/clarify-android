package ru.sla.clarify.feature.chat.thread.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Commit

@Immutable
data class Branch(
  val id: Id,
  val conversationId: Conversation.Id,
  val parentBranchId: Id,
  val branchedFromCommitId: Commit.Id,
  val name: String,
  val status: Status,
  val createdAt: Long,
  val createdByUid: UserId,
  val mergeRequest: MergeRequest? = null,
  val mergedAt: Long? = null,
  val mergedIntoBranchId: Id? = null
) {
  @JvmInline
  value class Id(val value: String)

  enum class Status(val value: String) {
    Active("active"),
    MergeInProgress("mergeInProgress"),
    Merged("merged");

    companion object {
      fun fromValue(value: String): Status {
        return entries.firstOrNull { it.value == value }
          ?: error("unexpected branch status: $value")
      }
    }
  }

  /**
   * Active merge proposal for a branch. While present, the branch is in [Status.MergeInProgress]
   * and accepts no new commits. Once [approvedByUids] covers all conversation participants,
   * the branch transitions to [Status.Merged].
   */
  @Immutable
  data class MergeRequest(
    val initiatorUid: UserId,
    val requestedAt: Long,
    val approvedByUids: Set<UserId>
  )
}
