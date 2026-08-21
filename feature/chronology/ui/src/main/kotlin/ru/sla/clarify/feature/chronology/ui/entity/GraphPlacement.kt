package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset

/**
 * Разложенный граф: где стоят плашки, докуда простирается содержимое и какие связи между узлами.
 *
 * Всё в пикселях и в координатах полотна. Считается целиком одной чистой функцией, поэтому
 * проверяется юнит-тестом — именно в этой арифметике жили все регрессии раскладки.
 *
 * @param nodes левые верхние углы узлов, в порядке модели
 * @param bounds объединение прямоугольников узлов: единственный источник истины о протяжённости
 * @param edges связи между соседними узлами дорожек
 * @param centreSpanX отрезок центров плашек по X, парный к [bounds] по краям
 */
@Immutable
data class GraphPlacement(
  val nodes: List<IntOffset>,
  val bounds: Rect,
  val edges: List<GraphEdge>,
  val centreSpanX: ClosedFloatingPointRange<Float>
) {

  /** Пуст ли граф: панорамировать нечего. */
  val isEmpty: Boolean
    get() = nodes.isEmpty()

  companion object {

    /** Пустой граф: панорамировать нечего, камера остаётся в нуле. */
    val Empty = GraphPlacement(
      nodes = emptyList(),
      bounds = Rect.Zero,
      edges = emptyList(),
      centreSpanX = 0f..0f
    )
  }
}
