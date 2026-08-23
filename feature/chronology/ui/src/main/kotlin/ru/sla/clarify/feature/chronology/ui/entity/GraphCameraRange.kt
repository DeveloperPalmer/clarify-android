package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/**
 * Где камере разрешено быть — по обеим осям сразу.
 *
 * Одно определение на чтение, на запись и на пере-зажатие после раскладки. Раньше диапазон
 * существовал только выражением внутри чтения камеры, поэтому запись о нём не знала и уводила
 * камеру сколь угодно далеко за содержимое.
 *
 * Вырожденный диапазон здесь — норма, а не краевой случай: пока дорожка одна, содержимое по
 * вертикали помещается целиком, и [panRangeOf] схлопывает ось Y в одно центрирующее значение.
 *
 * @param x допустимый сдвиг содержимого по оси времени
 * @param y допустимый сдвиг содержимого по дорожкам
 */
@Immutable
data class GraphCameraRange(
  val x: ClosedFloatingPointRange<Float>,
  val y: ClosedFloatingPointRange<Float>
) {

  /** Где камера стоит, пока её не двигали: начало истории в центре экрана. */
  val rest: Offset
    get() = Offset(x.endInclusive, y.endInclusive)

  companion object {

    /** Пустой граф: панорамировать нечего, камера остаётся в нуле. */
    val Empty = GraphCameraRange(x = 0f..0f, y = 0f..0f)
  }
}
