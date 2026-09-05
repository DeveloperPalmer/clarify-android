package ru.sla.clarify.entity.chat

import androidx.compose.runtime.Immutable
import ru.sla.resourcerefs.TextRef

@Immutable
sealed interface Conversation {
  val id: Id
  val lastCommit: String?
  val lastCommitAt: TextRef?
  val lastCommitTimestamp: Long
  val unreadCount: Long

  @JvmInline
  value class Id(val value: String)

  @Immutable
  data class Direct(
    override val id: Id,
    override val lastCommit: String?,
    override val lastCommitAt: TextRef?,
    override val lastCommitTimestamp: Long,
    override val unreadCount: Long,
    val peer: Peer
  ) : Conversation

  @Immutable
  data class Group(
    override val id: Id,
    override val lastCommit: String?,
    override val lastCommitAt: TextRef?,
    override val lastCommitTimestamp: Long,
    override val unreadCount: Long,
    val name: String,
    val lastCommitSenderName: String?
  ) : Conversation
}
