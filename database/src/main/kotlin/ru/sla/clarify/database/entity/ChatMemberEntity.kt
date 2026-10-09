package ru.sla.clarify.database.entity

import androidx.room3.Entity
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Member

/**
 * Первым в ключе стоит `conversationId`: участников выбирают по разговору, и ключ служит этому
 * запросу индексом. Порядок столбцов строки от этого не меняется.
 */
@Entity(
  tableName = "ChatMember",
  primaryKeys = ["conversationId", "id"]
)
data class ChatMemberEntity(
  val id: Member.Id,
  val conversationId: Conversation.Id
)
