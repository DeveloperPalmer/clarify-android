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
  val createdByUid: UserId
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
}
