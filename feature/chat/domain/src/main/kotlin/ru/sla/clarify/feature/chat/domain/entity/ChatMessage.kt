package ru.sla.clarify.feature.chat.domain.entity

import androidx.compose.runtime.Immutable
import java.time.LocalDateTime

@Immutable
data class ChatMessage(
  val msgId: String,
  val peerId: String,
  val senderId: String,
  val text: String,
  val timestamp: LocalDateTime,
  val isSelf: Boolean,
  val status: Status
) {
  enum class Status {
    Sending,
    Sent,
    Failed
  }
}
