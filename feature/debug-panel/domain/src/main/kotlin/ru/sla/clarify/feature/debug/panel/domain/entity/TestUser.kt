package ru.sla.clarify.feature.debug.panel.domain.entity

import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.randomUuid
import ru.sla.clarify.feature.debug.panel.domain.ensureJsonSignature
import ru.sla.clarify.feature.debug.panel.domain.parse
import ru.sla.clarify.feature.debug.panel.domain.requireHttpUrl
import ru.sla.clarify.feature.debug.panel.domain.requireString

@JvmInline
value class TestUser private constructor(val value: String) {
  companion object {
    operator fun invoke(raw: String): EitherNel<UserJsonError, User> = either {
      val obj = parse(raw.trim()) ?: raise(nonEmptyListOf(UserJsonError.MalformedJson))
      zipOrAccumulate(
        { ensureJsonSignature(obj, userFields) },
        { requireString(obj, FIELD_EMAIL) },
        { requireString(obj, FIELD_DISPLAY_NAME) },
        { requireHttpUrl(obj, FIELD_PHOTO_URL) }
      ) { _, email, displayName, photoUrl ->
        User(
          id = UserId("test_${randomUuid()}"),
          email = Email(email),
          photoUrl = photoUrl,
          displayName = displayName
        )
      }
    }
  }
}

private val userFields = setOf(
  FIELD_EMAIL,
  FIELD_DISPLAY_NAME,
  FIELD_PHOTO_URL
)

private const val FIELD_DISPLAY_NAME = "displayName"
private const val FIELD_EMAIL = "email"
private const val FIELD_PHOTO_URL = "photoUrl"
