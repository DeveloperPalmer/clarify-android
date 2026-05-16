package ru.sla.clarify.feature.login.entity

import ru.sla.clarify.auth.session.domain.entity.AuthTokens
import ru.sla.clarify.auth.session.domain.entity.UserId

data class AuthResult(
  val userId: UserId,
  val tokens: AuthTokens
)
