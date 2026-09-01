package ru.sla.atlas.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Разложенный граф: где стоят узлы и докуда простирается содержимое.
 *
 * Всё в пикселях и в координатах полотна. Считается целиком одной чистой функцией, поэтому
 * проверяется юнит-тестом — именно в этой арифметике и живут регрессии раскладки.
 *
 * @param nodes левые верхние углы узлов, в порядке модели
 * @param sizes измеренные размеры узлов, в порядке модели. Держатся рядом с углами, а не выводятся
 *   из [centres] обратным счётом: удвоенная разность центра и угла даёт тот же размер лишь до
 *   округления, и вторая истина о размере разошлась бы с первой молча
 * @param bounds объединение прямоугольников узлов: единственный источник истины о протяжённости
 * @param centreSpanX отрезок центров плашек по X, парный к [bounds] по краям
 * @param centreSpanY отрезок центров плашек по Y, парный к [centreSpanX]: камера наводится на центры
 *   по обеим осям, и вторая ось не может обойтись перебором [centres] — диапазон спрашивают на
 *   каждом кадре жеста, а проход по всем узлам на кадр здесь и избегают
 * @param centres центры плашек, в порядке модели: куда наводиться, решает камера, а не раскладка
 */
@Immutable
data class Placement(
  val nodes: List<IntOffset>,
  val sizes: List<IntSize>,
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
    val Empty = Placement(
      nodes = emptyList(),
      sizes = emptyList(),
      bounds = Rect.Zero,
      centreSpanX = 0f..0f,
      centreSpanY = 0f..0f,
      centres = emptyList()
    )
  }
}
