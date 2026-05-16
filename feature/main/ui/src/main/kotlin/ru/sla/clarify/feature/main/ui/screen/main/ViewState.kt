package ru.sla.clarify.feature.main.ui.screen.main

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState

@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.Ready
)
