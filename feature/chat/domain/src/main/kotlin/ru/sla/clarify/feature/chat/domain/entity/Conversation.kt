package ru.sla.clarify.feature.chat.domain.entity

import androidx.compose.runtime.Immutable

@Immutable
data class Conversation(
  val id: Id,
  val peer: Peer,
  val lastMessage: String?,
  val lastMessageTimestamp: Long,
  val unreadCount: Long
) {
  @JvmInline
  value class Id(val value: String)

  @Immutable
  data class Peer(
    val id: String,
    val name: String?,
    val faceUrl: String?
  )
}
