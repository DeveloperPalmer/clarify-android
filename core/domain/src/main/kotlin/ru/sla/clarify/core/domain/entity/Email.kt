package ru.sla.clarify.core.domain.entity

import androidx.compose.runtime.Immutable
import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate

@Immutable
data class Email(val value: String) {
  companion object {
    fun validate(
      peerEmail: String,
      userEmail: Email?
    ): EitherNel<Error, Email> = either {
      val trimmedValue = peerEmail.trim()
      zipOrAccumulate(
        { ensure(trimmedValue.isNotBlank()) { Error.Empty } },
        { ensure(trimmedValue.length >= MIN_LENGTH) { Error.TooShort } },
        { ensure(trimmedValue.length <= MAX_LENGTH) { Error.TooLong } },
        { ensure(!trimmedValue.equals(userEmail?.value, ignoreCase = true)) { Error.SelfEmail } },
        { ensure(allowedPattern.matches(trimmedValue)) { Error.Invalid } }
      ) { _, _, _, _, _ ->
        Email(value = trimmedValue)
      }
    }
  }

  @Immutable
  sealed interface Error {
    @Immutable
    data object Empty : Error

    @Immutable
    data object TooShort : Error

    @Immutable
    data object TooLong : Error

    @Immutable
    data object SelfEmail : Error

    @Immutable
    data object Invalid : Error
  }
}

private const val MIN_LENGTH = 5
private const val MAX_LENGTH = 100
private val allowedPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
