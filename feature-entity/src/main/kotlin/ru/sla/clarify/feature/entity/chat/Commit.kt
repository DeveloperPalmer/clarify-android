package ru.sla.clarify.feature.entity.chat

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId
import java.time.LocalDateTime

@Immutable
sealed interface Commit {

  @JvmInline
  value class Id(val value: String)

  val id: Id
  val senderId: UserId
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
    override val senderId: UserId,
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
