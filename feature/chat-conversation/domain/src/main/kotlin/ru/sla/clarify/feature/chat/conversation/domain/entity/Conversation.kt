package ru.sla.clarify.feature.chat.conversation.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.resourcerefs.TextRef

@Immutable
sealed interface Conversation {
  val id: Id
  val lastCommit: String?
  val lastCommitAt: TextRef?
  val lastCommitTimestamp: Long
  val unreadCount: Long

  data class Direct(
    override val id: Id,
    override val lastCommit: String?,
    override val lastCommitAt: TextRef?,
    override val lastCommitTimestamp: Long,
    override val unreadCount: Long,
    val peer: Peer
  ) : Conversation

  @JvmInline
  value class Id(val value: String)
}
