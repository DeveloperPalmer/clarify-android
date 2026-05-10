package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import ru.sla.clarify.feature.chronology.domain.ChronologyGraph

/**
 * Раскладка хронологии: цепочка идёт слева направо (по depth),
 * параллельные ветки откладываются вниз (по branchIndex).
 * Каждый узел кладётся в (depth * (nodeWidth + hSpacing), branchIndex * (nodeHeight + vSpacing)).
 *
 * Returned [Offset] указывает левый верхний угол узла; общий [Size]
 * описывает прямоугольник всех узлов вместе со spacing-полями по краям.
 */
@Immutable
data class ChronologyLayout(
  val positions: Map<String, Offset>,
  val contentSize: Size
) {
  companion object {
    val Empty = ChronologyLayout(positions = emptyMap(), contentSize = Size.Zero)
  }
}

internal fun layoutGraph(
  graph: ChronologyGraph,
  nodeWidthPx: Float,
  nodeHeightPx: Float,
  hSpacingPx: Float,
  vSpacingPx: Float
): ChronologyLayout {
  if (graph.nodes.isEmpty()) return ChronologyLayout.Empty

  val positions = HashMap<String, Offset>(graph.nodes.size)
  var maxDepth = 0
  var maxBranch = 0
  graph.nodes.forEach { node ->
    val x = node.depth * (nodeWidthPx + hSpacingPx)
    val y = node.branchIndex * (nodeHeightPx + vSpacingPx)
    positions[node.message.id.value] = Offset(x, y)
    if (node.depth > maxDepth) maxDepth = node.depth
    if (node.branchIndex > maxBranch) maxBranch = node.branchIndex
  }

  val width = (maxDepth + 1) * nodeWidthPx + maxDepth * hSpacingPx
  val height = (maxBranch + 1) * nodeHeightPx + maxBranch * vSpacingPx
  return ChronologyLayout(
    positions = positions,
    contentSize = Size(width = width, height = height)
  )
}
