package ru.sla.clarify.feature.chat.domain.entity

import androidx.compose.runtime.Immutable

@Immutable
data class Conversation(
  val id: String,
  val peer: Peer,
  val lastMessage: String?,
  val lastMessageTimestamp: Long,
  val unreadCount: Long
) {
  @Immutable
  data class Peer(
    val id: String,
    val name: String?,
    val faceUrl: String?
  )
}
