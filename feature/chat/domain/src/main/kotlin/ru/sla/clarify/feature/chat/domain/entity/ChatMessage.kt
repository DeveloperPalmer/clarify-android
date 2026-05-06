package ru.sla.clarify.feature.chat.domain.entity

data class ChatMessage(
  val msgId: String,
  val peerUserId: String,
  val senderId: String,
  val text: String,
  val timestamp: Long,
  val isSelf: Boolean,
  val status: Status
) {
  enum class Status {
    Sending,
    Sent,
    Failed
  }
}
