package ru.sla.clarify.feature.chronology.ui.entity

import ru.sla.atlas.entity.NodeScale
import ru.sla.atlas.entity.TimeGap
import ru.sla.clarify.feature.chronology.ui.mapper.toTimeGap
import java.time.Duration

internal object GraphNodeScale : NodeScale<GraphNode> {

  override fun gapOf(duration: Duration): TimeGap {
    return duration.toTimeGap()
  }

  // Перечисление здесь неизбежно: пауза лежит в каждом роде узла своим полем, и общего `copy` у
  // запечатанного типа нет. Зато оно полное — род, забытый в этом `when`, не компилируется, а
  // забытый в заглушке просто уехал бы на экран с чужим зазором.
  override fun withGap(node: GraphNode, gap: TimeGap): GraphNode {
    return when (node) {
      is GraphNode.Episode -> node.copy(gap = gap)
      is GraphNode.Fork -> node.copy(gap = gap)
      is GraphNode.Merge -> node.copy(gap = gap)
      is GraphNode.Front -> node.copy(gap = gap)
    }
  }
}
