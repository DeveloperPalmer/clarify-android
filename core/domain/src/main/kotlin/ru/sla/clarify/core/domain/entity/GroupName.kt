package ru.sla.clarify.core.domain.entity

import androidx.compose.runtime.Immutable
import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate

@Immutable
data class GroupName(val value: String) {
  companion object {
    fun validate(name: String): EitherNel<Error, GroupName> = either {
      val trimmedValue = name.trim()
      zipOrAccumulate(
        { ensure(trimmedValue.isNotBlank()) { Error.Empty } },
        { ensure(trimmedValue.length <= MAX_LENGTH) { Error.TooLong } }
      ) { _, _ ->
        GroupName(value = trimmedValue)
      }
    }
  }

  @Immutable
  sealed interface Error {
    @Immutable
    data object Empty : Error

    @Immutable
    data object TooLong : Error
  }
}

private const val MAX_LENGTH = 50
