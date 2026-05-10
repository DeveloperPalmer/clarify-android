package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.entity.UiError
import ru.sla.clarify.feature.chronology.domain.ChronologyGraph

@Immutable
data class ViewState(
  val peerId: String = "",
  val graph: ChronologyGraph = ChronologyGraph.Empty,
  val contentLoadState: ContentLoadState = ContentLoadState.Ready,
  val dialogError: UiError? = null,
  val snackbarError: UiError? = null
)
