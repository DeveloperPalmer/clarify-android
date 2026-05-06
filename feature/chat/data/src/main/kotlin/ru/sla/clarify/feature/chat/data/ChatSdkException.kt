package ru.sla.clarify.feature.chat.data

internal class ChatSdkException(
  val errorCode: Int,
  message: String?
) : RuntimeException("Chat SDK error $errorCode: ${message.orEmpty()}")
