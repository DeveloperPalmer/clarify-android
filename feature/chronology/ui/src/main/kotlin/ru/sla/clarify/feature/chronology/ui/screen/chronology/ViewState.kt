package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.runtime.Immutable
import ru.sla.atlas.entity.Node
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chronology.ui.entity.Chronology

@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.Ready,
  val chronology: Chronology = Chronology(),
  val selectedNodeId: Node.Id? = null,
  val debugOverlayAvailable: Boolean = false,
  val debugOverlayVisible: Boolean = false
)
