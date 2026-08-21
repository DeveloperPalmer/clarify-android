package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.entity.chat.Branch

@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.Ready,
  val branches: List<Branch> = emptyList(),
  val debugOverlayVisible: Boolean = false
)
