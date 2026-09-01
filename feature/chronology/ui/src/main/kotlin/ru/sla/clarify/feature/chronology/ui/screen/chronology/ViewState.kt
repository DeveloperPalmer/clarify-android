package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chronology.ui.entity.Chronology
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeSelection

@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.Ready,
  val chronology: Chronology = Chronology(),
  val selectedNode: GraphNodeSelection? = null,
  val debugOverlayAvailable: Boolean = false,
  val debugOverlayVisible: Boolean = false
)
