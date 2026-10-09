package ru.sla.clarify.database.entity

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Conversation

data class GroupRow(
  val id: Conversation.Id,
  val name: String?,
  val ownerId: UserId?,
  val lastCommit: String?,
  val lastCommitSenderId: UserId?,
  val lastCommitTimestamp: Long,
  val unreadCount: Long,
  val memberCount: Long
)
