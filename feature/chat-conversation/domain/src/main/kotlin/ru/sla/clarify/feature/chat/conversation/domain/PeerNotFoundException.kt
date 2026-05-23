package ru.sla.clarify.feature.chat.conversation.domain

import ru.sla.clarify.core.domain.entity.Email

class PeerNotFoundException(val email: Email) : RuntimeException(
  "No user found with email: ${email.value}"
)
