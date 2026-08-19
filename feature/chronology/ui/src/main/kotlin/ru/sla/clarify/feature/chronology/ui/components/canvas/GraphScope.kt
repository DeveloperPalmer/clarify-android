package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/** Область содержимого [GraphCanvas]: узлы размещаются в координатах полотна, а не потоком. */
@LayoutScopeMarker
interface GraphScope {

  /**
   * Ставит узел центром в точку `(x, y)` полотна.
   *
   * Именно центром, а не левым верхним углом: узел «сидит» на дорожке, и его высота не должна
   * влиять на то, где проходит линия ветки.
   */
  fun Modifier.nodeAt(x: Dp, y: Dp): Modifier
}

internal object GraphScopeInstance : GraphScope {
  override fun Modifier.nodeAt(x: Dp, y: Dp): Modifier = this.then(GraphNodePosition(x, y))
}

internal class GraphNodePosition(val x: Dp, val y: Dp) : ParentDataModifier {
  override fun Density.modifyParentData(parentData: Any?): Any = this@GraphNodePosition

  override fun equals(other: Any?): Boolean {
    return other is GraphNodePosition && other.x == x && other.y == y
  }

  override fun hashCode(): Int = 31 * x.hashCode() + y.hashCode()
}
