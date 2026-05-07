package ru.sla.clarify.feature.chat.domain.entity

class ChatSdkException(
  errorCode: Int,
  message: String?
) : RuntimeException("Chat SDK error $errorCode: ${message.orEmpty()}")
