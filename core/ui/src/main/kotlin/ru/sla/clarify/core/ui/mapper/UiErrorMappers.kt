package ru.sla.clarify.core.ui.mapper

import ru.kode.log.asLog
import ru.kode.resourcerefs.resRef
import ru.kode.resourcerefs.strRef
import ru.sla.clarify.core.domain.entity.ApiError
import ru.sla.clarify.core.domain.entity.ConnectivityError
import ru.sla.clarify.core.domain.logError
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.entity.UiError
import ru.sla.clarify.core.ui.entity.UiMessage

/**
 * Converts a commonly known domain error into a ui error.
 * If an error is not known and unexpected, it is converted to a generic error using [toGenericUiError] function
 */
fun Throwable.toAppUiError(
  primaryAction: UiMessage.Action? = null
): UiError {
  logError { this.asLog() }
  return when (this) {
    is ConnectivityError -> this.toUiError(primaryAction)
    is ApiError -> this.toUiError(primaryAction)
    else -> toGenericUiError(primaryAction)
  }
}

private fun ApiError.toUiError(
  primaryAction: UiMessage.Action? = null
): UiError {
  return UiError(
    message = UiMessage(
      description = strRef(this.description),
      title = resRef(id = R.string.error_something_went_wrong_title),
      imageResource = null,
      primaryAction = primaryAction
    ),
    cause = this
  )
}

private fun ConnectivityError.toUiError(
  primaryAction: UiMessage.Action? = null
): UiError {
  return when (this) {
    is ConnectivityError.NoConnection -> {
      UiError(
        message = UiMessage(
          description = resRef(id = R.string.error_no_connection_description),
          title = resRef(id = R.string.error_no_connection_title),
          imageResource = null,
          primaryAction = primaryAction
        ),
        cause = this
      )
    }
    is ConnectivityError.SSLError -> {
      UiError(
        message = UiMessage(
          description = resRef(id = R.string.error_ssl_error_description),
          title = resRef(id = R.string.error_ssl_error_title),
          imageResource = null,
          primaryAction = primaryAction
        ),
        cause = this
      )
    }
    is ConnectivityError.TimeOut -> {
      UiError(
        message = UiMessage(
          description = resRef(id = R.string.error_server_timeout_description),
          title = resRef(id = R.string.error_server_timeout_title),
          imageResource = null,
          primaryAction = primaryAction
        ),
        cause = this
      )
    }
  }
}

fun Throwable.toGenericUiError(
  primaryAction: UiMessage.Action?
): UiError {
  return UiError(
    message = UiMessage(
      description = resRef(id = R.string.error_something_went_wrong_description),
      title = resRef(id = R.string.error_something_went_wrong_title),
      imageResource = null,
      primaryAction = primaryAction
    ),
    cause = this
  )
}
