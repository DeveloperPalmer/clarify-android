package ru.sla.clarify.feature.chronology.domain

import androidx.compose.runtime.Immutable
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage

@Immutable
data class ChronologyGraph(
  val nodes: List<ChronologyNode>,
  val edges: List<ChronologyEdge>
) {
  companion object {
    val Empty = ChronologyGraph(
      nodes = emptyList(),
      edges = emptyList()
    )
  }
}

@Immutable
data class ChronologyNode(
  val message: ChatMessage,
  val depth: Int,
  val branchIndex: Int
)

@Immutable
data class ChronologyEdge(
  val fromMsgId: String,
  val toMsgId: String
)
