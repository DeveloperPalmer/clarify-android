package ru.sla.clarify.core.domain.entity

import androidx.compose.runtime.Immutable
import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate

@Immutable
data class Email(val value: String) {
  companion object {
    fun validate(value: String): EitherNel<Error, Email> = either {
      val trimmedValue = value.trim()
      zipOrAccumulate(
        { ensure(trimmedValue.isNotBlank()) { Error.Empty } },
        { ensure(allowedPattern.matches(trimmedValue)) { Error.Invalid } }
      ) { _, _ ->
        Email(value = trimmedValue)
      }
    }
  }

  @Immutable
  sealed interface Error {
    @Immutable
    data object Empty : Error

    @Immutable
    data object Invalid : Error
  }
}

private val allowedPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
