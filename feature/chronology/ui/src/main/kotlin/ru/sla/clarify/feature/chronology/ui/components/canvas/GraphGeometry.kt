package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Где на полотне стоят узлы графа из [nodes].
 *
 * Только пространство: ни состояния, ни времени, ни плотности экрана, ни Compose. Всё, что здесь
 * считается, зависит исключительно от модели, поэтому проверяется юнит-тестом и одинаково доступно
 * раскладке, отрисовке рёбер, мини-карте и попаданию пальцем.
 *
 * Координаты отсчитываются от левого верхнего угла полотна. Начало полотна — не начало экрана:
 * сдвиг под вьюпорт добавляет камера, и геометрия о нём не знает.
 *
 * @param nodes узлы в хронологическом порядке
 */
@JvmInline
internal value class GraphGeometry(private val nodes: List<GraphNode>) {

  /**
   * Занятые дорожки, от самой верхней до самой нижней.
   *
   * Магистраль входит всегда, даже когда на ней нет ни одного узла: пустая переписка — это всё
   * ещё переписка, у которой просто нет истории.
   *
   * @return диапазон номеров дорожек, включающий ноль
   */
  fun laneRange(): IntRange {
    var first = 0
    var last = 0
    nodes.forEach { node ->
      if (node.lane < first) first = node.lane
      if (node.lane > last) last = node.lane
    }
    return first..last
  }

  /**
   * Смещение дорожки по Y, отсчитанное от верха полотна.
   *
   * Магистраль не привязана к константе: её место определяется тем, сколько дорожек занято сверху.
   * Иначе высота полотна и положение линии были бы двумя независимыми истинами об одном графе.
   *
   * @param lane номер дорожки
   * @return смещение центра дорожки от верха полотна
   */
  fun laneYOf(lane: Int): Dp {
    return LANE_STEP * (lane - laneRange().first) + LANE_STEP / 2
  }
}

/**
 * Смещения левых краёв узлов, накопленные по зазорам и ширинам.
 *
 * Зазор отделяет плашки, а не центры: плашка бывает шириной до 180 dp, и мерить сорок дюймовых
 * пикселей от центра до центра значило бы положить узлы друг на друга. Отсюда же берётся длина
 * ребра — она равна зазору по построению.
 *
 * Работает в пикселях, а не в [Dp]: ширины приходят из измерения, а переводить их в [Dp] только
 * чтобы вернуть назад — лишний круг.
 *
 * @param gaps зазор перед каждым узлом
 * @param widths измеренные ширины узлов
 * @return смещения левых краёв в порядке узлов
 */
internal fun leftOffsetsOf(gaps: List<Float>, widths: List<Float>): List<Float> {
  var left = 0f
  return gaps.mapIndexed { index, gap ->
    left += gap
    val result = left
    left += widths[index]
    result
  }
}

/**
 * Зазор перед узлом.
 *
 * Пять дискретных значений вместо реальной длительности: абсолютное время растянуло бы ночную
 * паузу на километры пустоты, а одинаковый зазор стёр бы паузы вовсе. Дискретный зазор оставляет
 * порядок величины, а точную длительность выводит подпись.
 *
 * @param gap квантованная пауза перед узлом
 * @return расстояние от предыдущей плашки
 */
internal fun stepWidthOf(gap: TimeGap): Dp {
  return when (gap) {
    TimeGap.Minutes -> 40.dp
    TimeGap.Hour -> 68.dp
    TimeGap.Hours -> 96.dp
    TimeGap.Day -> 124.dp
    TimeGap.Long -> 152.dp
  }
}

/**
 * Допустимый сдвиг содержимого по одной оси.
 *
 * Камера — это сдвиг содержимого: `экран = полотно + камера`. Чтобы начало содержимого встало у
 * начала экрана, нужен сдвиг `-min`; чтобы конец встал у конца экрана — `viewport - max`.
 *
 * Когда содержимое короче экрана, границы схлопываются в одно центрирующее значение: прижимать к
 * краю то, что помещается целиком, незачем.
 *
 * @param min начало содержимого в координатах полотна
 * @param max конец содержимого в координатах полотна
 * @param viewport размер видимой области по той же оси
 * @return диапазон сдвига, вырожденный в точку, когда содержимое помещается целиком
 */
internal fun panRangeOf(min: Float, max: Float, viewport: Float): ClosedFloatingPointRange<Float> {
  val lower = viewport - max
  // Именно `0f - min`, а не `-min`: у нуля унарный минус даёт отрицательный нуль, и граница
  // печаталась как «-0.0».
  val upper = 0f - min
  if (lower <= upper) {
    return lower..upper
  }
  val centered = (viewport - (max - min)) / 2f - min
  return centered..centered
}

private val LANE_STEP: Dp = 104.dp

internal val EDGE_WIDTH: Dp = 2.dp
