package ru.sla.clarify.database.entity

import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Member

data class DirectConversationRow(
  val id: Conversation.Id,
  val type: String,
  val lastCommit: String?,
  val lastCommitTimestamp: Long,
  val unreadCount: Long,
  val peerId: Member.Id,
  val peerDisplayName: String,
  val peerPhotoUrl: String?
)
