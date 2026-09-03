package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.CoroutineScope
import ru.sla.atlas.entity.Node
import ru.sla.atlas.entity.NodeAccent
import ru.sla.clarify.feature.chronology.ui.components.canvas.MergeCeremonyState
import ru.sla.clarify.feature.chronology.ui.components.node.EpisodeNode
import ru.sla.clarify.feature.chronology.ui.components.node.ForkNode
import ru.sla.clarify.feature.chronology.ui.components.node.FrontNode
import ru.sla.clarify.feature.chronology.ui.components.node.GlyphNode
import ru.sla.clarify.feature.chronology.ui.components.node.MergeNode
import ru.sla.clarify.feature.chronology.ui.entity.Chronology
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode

@Composable
internal fun GraphNode(
  modifier: Modifier = Modifier,
  chronology: Chronology,
  mergeCeremonyState: MergeCeremonyState,
  selectedNode: Node.Id?,
  node: GraphNode,
  level: GraphLevel,
  accent: NodeAccent,
  scope: CoroutineScope,
  hapticFeedback: HapticFeedback,
  onClick: (Node.Id) -> Unit
) {
  return when (node) {
    is GraphNode.Front -> {
      FrontNode(
        modifier = modifier,
        hasCaption = level == GraphLevel.LOD0
      )
    }
    is GraphNode.Fork -> {
      ForkNode(
        modifier = modifier,
        color = accent.color
      )
    }
    is GraphNode.Merge -> {
      val mergedBranch = chronology.graph.branchMergedAt(node.id)
      MergeNode(
        modifier = modifier,
        state = GraphNode.Merge.Status.Done,
        level = level,
        accent = accent,
        ceremony = {
          mergeCeremonyState
            .takeIf { it.branch.value == mergedBranch }
            ?.frame
            ?.value
        },
        onClick = mergedBranch?.let { branchId ->
          {
            mergeCeremonyState.play(
              scope = scope,
              branchId = branchId,
              onFinishAnimation = { hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm) }
            )
          }
        }
      )
    }
    is GraphNode.Episode -> when (level) {
      GraphLevel.LOD0 -> {
        EpisodeNode(
          modifier = modifier.graphicsLayer { alpha = if (node.id == selectedNode) 0f else 1f },
          time = node.time,
          count = node.count,
          snippet = node.text,
          myShare = node.myShare,
          unreadCount = node.unreadCount,
          dim = node.dim,
          onClick = { onClick(node.id) }
        )
      }
      GraphLevel.LOD1 -> {
        GlyphNode(
          modifier = modifier.graphicsLayer { alpha = if (node.dim) 0.6f else 1f },
          color = accent.color,
          unreadCount = node.unreadCount
        )
      }
    }
  }
}
