package ru.sla.clarify.feature.chat.conversation.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.feature.entity.chat.Peer

@Immutable
sealed interface Conversation {
  val id: Id
  val lastMessage: String?
  val lastMessageTimestamp: Long
  val unreadCount: Long

  data class Direct(
    override val id: Id,
    override val lastMessage: String?,
    override val lastMessageTimestamp: Long,
    override val unreadCount: Long,
    val peer: Peer
  ) : Conversation

  @JvmInline
  value class Id(val value: String)
}
