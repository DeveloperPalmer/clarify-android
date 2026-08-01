package ru.sla.clarify.entity.chat

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId
import java.time.LocalDateTime

@Immutable
sealed interface Commit {

  @JvmInline
  value class Id(val value: String)

  val id: Id
  val senderId: UserId
  val timestamp: LocalDateTime
  val isSelf: Boolean
  val status: Status

  @Immutable
  data class Message(
    override val id: Id,
    override val timestamp: LocalDateTime,
    override val senderId: UserId,
    override val isSelf: Boolean,
    override val status: Status,
    val text: String,
    val editedAt: LocalDateTime? = null
  ) : Commit

  @Immutable
  data class InviteMember(
    override val id: Id,
    override val timestamp: LocalDateTime,
    override val senderId: UserId,
    override val isSelf: Boolean,
    override val status: Status,
    val invitedId: UserId
  ) : Commit

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

  enum class Type(val value: String) {
    Text("text"),
    InviteMember("inviteMember");

    companion object {
      fun fromValue(value: String): Type {
        return entries.firstOrNull { it.value == value } ?: error("unexpected type: $value")
      }
    }
  }
}
