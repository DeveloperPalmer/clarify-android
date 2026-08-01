package ru.sla.clarify.core.domain.entity

import androidx.compose.runtime.Immutable
import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate

@Immutable
data class EditedMessage(val value: String) {
  companion object {
    fun validate(
      text: String,
      original: String?
    ): EitherNel<Error, EditedMessage> = either {
      val trimmedValue = text.trim()
      zipOrAccumulate(
        { ensure(trimmedValue.isNotBlank()) { Error.Empty } },
        { ensure(trimmedValue != original) { Error.Unchanged } }
      ) { _, _ ->
        EditedMessage(value = trimmedValue)
      }
    }
  }

  @Immutable
  sealed interface Error {
    @Immutable
    data object Empty : Error

    @Immutable
    data object Unchanged : Error
  }
}
