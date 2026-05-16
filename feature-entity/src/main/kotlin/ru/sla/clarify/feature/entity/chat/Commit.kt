package ru.sla.clarify.feature.entity.chat

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

@Immutable
sealed interface Commit {

  @JvmInline
  value class Id(val value: String)

  val id: Id
  val parentId: Id?
  val peerId: String
  val senderId: String
  val text: String
  val timestamp: LocalDateTime
  val isSelf: Boolean
  val status: Status
  val colorHex: String

  @Immutable
  data class Message(
    override val id: Id,
    override val timestamp: LocalDateTime,
    override val colorHex: String,
    override val parentId: Id?,
    override val peerId: String,
    override val senderId: String,
    override val text: String,
    override val isSelf: Boolean,
    override val status: Status
  ) : Commit

  enum class Status {
    Sending,
    Sent,
    Failed
  }
}
