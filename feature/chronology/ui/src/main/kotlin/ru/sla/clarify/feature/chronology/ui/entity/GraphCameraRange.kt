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
 * Вырожденный диапазон здесь — норма, а не краевой случай: у графа из единственного узла обе оси
 * схлопываются в точку, и правило остановки инерции обязано считать это упором, а не ошибкой.
 * Содержимое, которое помещается в экран целиком, при этом не вырождается — ему разрешено гулять
 * внутри экрана, иначе пинч над ним не удержал бы точку под пальцами.
 *
 * @param x допустимый сдвиг содержимого по оси времени
 * @param y допустимый сдвиг содержимого по дорожкам
 */
@Immutable
data class GraphCameraRange(
  val x: ClosedFloatingPointRange<Float>,
  val y: ClosedFloatingPointRange<Float>
) {

  /**
   * Загоняет камеру внутрь диапазона.
   *
   * @param camera желаемое положение
   * @return ближайшее допустимое
   */
  fun clamp(camera: Offset): Offset {
    return Offset(x = camera.x.coerceIn(x), y = camera.y.coerceIn(y))
  }

  companion object {

    /** Пустой граф: панорамировать нечего, камера остаётся в нуле. */
    val Empty = GraphCameraRange(x = 0f..0f, y = 0f..0f)
  }
}
