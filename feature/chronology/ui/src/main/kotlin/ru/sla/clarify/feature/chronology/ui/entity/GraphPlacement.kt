package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
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
 * @param centreSpanX отрезок центров плашек по X, парный к [bounds] по краям
 * @param centreSpanY отрезок центров плашек по Y, парный к [centreSpanX]: камера наводится на центры
 *   по обеим осям, и вторая ось не может обойтись перебором [centres] — диапазон спрашивают на
 *   каждом кадре жеста, а проход по всем узлам на кадр здесь и избегают
 * @param centres центры плашек, в порядке модели: куда наводиться, решает камера, а не раскладка
 */
@Immutable
data class GraphPlacement(
  val nodes: List<IntOffset>,
  val bounds: Rect,
  val centreSpanX: ClosedFloatingPointRange<Float>,
  val centreSpanY: ClosedFloatingPointRange<Float>,
  val centres: List<Offset>
) {

  /** Пуст ли граф: панорамировать нечего. */
  val isEmpty: Boolean
    get() = nodes.isEmpty()

  companion object {

    /** Пустой граф: панорамировать нечего, камера остаётся в нуле. */
    val Empty = GraphPlacement(
      nodes = emptyList(),
      bounds = Rect.Zero,
      centreSpanX = 0f..0f,
      centreSpanY = 0f..0f,
      centres = emptyList()
    )
  }
}
