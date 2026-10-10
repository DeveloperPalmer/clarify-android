package ru.sla.clarify.database.entity

import androidx.room3.Entity
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Member

/**
 * Первым в ключе стоит `conversationId`: участников выбирают по разговору, и ключ служит этому
 * запросу индексом. Порядок столбцов строки от этого не меняется.
 *
 * Таблица `WITHOUT ROWID`: строка из двух коротких id целиком лежит в B-дереве ключа, а не дублируется
 * в отдельном автоиндексе поверх таблицы с `rowid`. Хранится вдвое меньше, запись обновляет одно дерево.
 */
@Entity(
  tableName = "ChatMember",
  primaryKeys = ["conversationId", "id"],
  withoutRowId = true,
)
data class ChatMemberEntity(
  val id: Member.Id,
  val conversationId: Conversation.Id
)
