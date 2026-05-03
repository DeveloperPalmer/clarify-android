package ru.sla.clarify.auth.session.domain.entity

data class AuthTokens(
  val accessToken: AccessToken,
  val refreshToken: RefreshToken,
  // See NOTE_ENCRYPTION_AND_TOKENS_REFRESH
  val updatedAt: Long
)

@JvmInline
value class AccessToken(val value: String)

@JvmInline
value class RefreshToken(val value: String)
