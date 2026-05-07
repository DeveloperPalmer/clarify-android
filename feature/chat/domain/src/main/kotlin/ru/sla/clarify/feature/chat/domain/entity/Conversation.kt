package ru.sla.clarify.feature.chat.domain.entity

data class Conversation(
  val id: String,
  val peer: Peer,
  val lastMessage: String?,
  val lastMessageTimestamp: Long,
  val unreadCount: Long
) {
  data class Peer(
    val id: String,
    val name: String?,
    val faceUrl: String?
  )
}
