package ru.sla.clarify.feature.login.ui.screen.splashintro

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.UiError

@Immutable
data class ViewState(
  val dialogError: UiError? = null,
  val snackbarError: UiError? = null,
  val processing: Boolean = false
)
