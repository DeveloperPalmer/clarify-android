package ru.sla.clarify.feature.chat.conversation.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.feature.entity.chat.Peer

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
}
