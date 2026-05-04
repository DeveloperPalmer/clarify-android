package ru.sla.clarify.feature.main.ui.screen.main

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.entity.UiError

@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.Ready,
  val dialogError: UiError? = null,
  val snackbarError: UiError? = null
)
