package ru.sla.clarify.database.adapter

import app.cash.sqldelight.ColumnAdapter
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage

object ChatMessageIdAdapter : ColumnAdapter<ChatMessage.Id, String> {
  override fun decode(databaseValue: String): ChatMessage.Id {
    return ChatMessage.Id(databaseValue)
  }

  override fun encode(value: ChatMessage.Id): String {
    return value.value
  }
}
