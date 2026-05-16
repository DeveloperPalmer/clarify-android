package ru.sla.clarify.feature.chat.conversation.data.entity

data class CustomMessagePayload(
  val text: String,
  val colorHex: String,
  val parentId: String?
)
