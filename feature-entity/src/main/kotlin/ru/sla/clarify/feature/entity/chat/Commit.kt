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
  val text: String?
  val timestamp: LocalDateTime
  val isSelf: Boolean
  val status: Status
  val colorHex: String?

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

  @Immutable
  data class InviteMember(
    override val id: Id,
    override val timestamp: LocalDateTime,
    override val senderId: UserId,
    override val isSelf: Boolean,
    override val status: Status,
    val invitedId: UserId
  ) : Commit {
    override val text: String? = null
    override val colorHex: String? = null
  }

  enum class Status(val value: String) {
    Sending("sending"),
    Sent("sent"),
    Read("read");

    companion object {
      fun fromValue(value: String): Status {
        return entries.firstOrNull { it.value == value } ?: error("unexpected status: $value")
      }
    }
  }
}
