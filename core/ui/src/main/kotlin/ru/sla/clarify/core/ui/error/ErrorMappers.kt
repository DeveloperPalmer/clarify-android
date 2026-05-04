package ru.sla.clarify.core.ui.error

import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.entity.UiError
import ru.sla.clarify.core.ui.entity.UiMessage
import ru.sla.resourcerefs.resRef

fun baseErrorMappers(error: Throwable): UiError {
  return composeErrorMapper(
    ::connectionErrorMapper,
    defaultMapper = ::defaultErrorMapper
  ).invoke(error)
}

fun connectionErrorMapper(error: Throwable): UiError {
  return UiError(
    cause = error,
    message = UiMessage(
      title = resRef(id = R.string.error_something_went_wrong_title),
      description = null,
      imageResource = null,
      primaryAction = null
    )
  )
}

fun defaultErrorMapper(error: Throwable): UiError {
  return UiError(
    cause = error,
    message = UiMessage(
      title = resRef(id = R.string.error_something_went_wrong_title),
      description = null,
      imageResource = null,
      primaryAction = null
    )
  )
}
