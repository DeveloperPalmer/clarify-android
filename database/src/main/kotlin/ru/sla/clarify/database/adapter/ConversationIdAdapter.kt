package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import ru.sla.clarify.entity.chat.Conversation

object ConversationIdAdapter : ColumnAdapter<Conversation.Id, String> {
  override fun decode(databaseValue: String): Conversation.Id {
    return Conversation.Id(databaseValue)
  }

  override fun encode(value: Conversation.Id): String {
    return value.value
  }
}
