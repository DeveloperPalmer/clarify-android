package ru.sla.clarify.feature.chat.domain.entity

data class Conversation(
  val peerUserId: String,
  val peerNickname: String?,
  val peerFaceUrl: String?,
  val lastMessage: String?,
  val lastMessageTimestamp: Long,
  val unreadCount: Int
)
