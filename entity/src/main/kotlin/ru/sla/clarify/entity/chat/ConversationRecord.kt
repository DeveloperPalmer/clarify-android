package ru.sla.clarify.entity.chat

import ru.sla.clarify.core.domain.entity.UserId

/**
 * Беседа такой, какой её держит источник истины: состав участников и денормализованное превью
 * последнего сообщения. В отличие от [Conversation], который собирается для экрана и знает про
 * непрочитанное и разрешённого собеседника, здесь только то, что приходит снаружи.
 */
data class ConversationRecord(
  val id: Conversation.Id,
  val type: Conversation.Type,
  val memberIds: List<Member.Id>,
  val name: String?,
  val ownerId: UserId?,
  val lastCommitText: String?,
  val lastCommitSenderId: UserId?,
  val lastCommitAtSeconds: Long
)
