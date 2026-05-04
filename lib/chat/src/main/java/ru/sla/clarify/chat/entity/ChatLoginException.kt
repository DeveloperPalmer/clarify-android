package ru.sla.clarify.chat.entity

class ChatLoginException(
  message: String? = null,
  cause: Throwable? = null
) : RuntimeException(message, cause)
