package ru.sla.clarify.core.ui.error

import ru.sla.clarify.core.ui.entity.UiError

typealias ErrorMapper = (Throwable) -> UiError

fun composeErrorMapper(
  vararg errorMappers: (Throwable) -> UiError?,
  defaultMapper: (Throwable) -> UiError = ::baseErrorMappers
): ErrorMapper {
  return { error -> errorMappers.firstNotNullOfOrNull { it(error) } ?: defaultMapper(error) }
}
