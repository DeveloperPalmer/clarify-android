package ru.sla.clarify.entity.chat

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.resourcerefs.TextRef

@Immutable
data class Branch(
  val id: Id,
  val conversationId: Conversation.Id,
  val parentBranchId: Id,
  val branchedFromCommitId: Commit.Id,
  val name: String,
  val lastCommit: String?,
  val lastCommitAt: TextRef?,
  val lastCommitTimestamp: Long,
  val unreadCount: Long,
  val createdAt: Long,
  val createdById: UserId,
  val mergeRequest: MergeRequest? = null
) {
  @Immutable
  data class Id(val value: String)

  @Immutable
  data class MergeRequest(
    val status: Status,
    val initiatorId: UserId,
    val requestedAt: Long,
    val approvedByIds: Set<UserId>,
    val mergedAt: Long? = null,
    val mergedIntoBranchId: Id? = null
  ) {
    enum class Status(val value: String) {
      Open("open"),
      ReadyToMerge("readyToMerge"),
      Merged("merged");

      companion object {
        fun fromValue(value: String): Status {
          return entries.firstOrNull { it.value == value }
            ?: error("unexpected merge request status: $value")
        }
      }
    }
  }
}
