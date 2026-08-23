package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.feature.chronology.ui.entity.EpisodeContent
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode

@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.Ready,
  val branches: List<Branch> = emptyList(),
  val nodes: List<GraphNode> = emptyList(),
  val episodeById: Map<GraphNode.Id, EpisodeContent> = emptyMap(),
  val debugOverlayAvailable: Boolean = false,
  val debugOverlayVisible: Boolean = false
)
