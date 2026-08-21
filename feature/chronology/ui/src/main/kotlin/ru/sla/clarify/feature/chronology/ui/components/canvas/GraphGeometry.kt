package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap
import kotlin.math.roundToInt

/**
 * Где на полотне лежат дорожки, если самая верхняя из занятых — [topLane].
 *
 * Только пространство: ни состояния, ни времени, ни плотности экрана, ни Compose. Конфигурация —
 * одно число, а не список узлов, именно поэтому обращение к дорожке стоит O(1): раньше здесь
 * пересчитывался диапазон дорожек на каждый вызов, и обход узлов выходил квадратичным.
 *
 * Координаты отсчитываются от левого верхнего угла полотна. Начало полотна — не начало экрана:
 * сдвиг под вьюпорт добавляет камера, и геометрия о нём не знает.
 *
 * @param topLane номер самой верхней занятой дорожки, см. [topLaneOf]
 */
@JvmInline
internal value class GraphGeometry(private val topLane: Int) {

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
    return LANE_STEP * (lane - topLane) + LANE_STEP / 2
  }
}

/**
 * Самая верхняя занятая дорожка.
 *
 * Магистраль учитывается всегда, даже когда на ней нет ни одного узла: пустая переписка — это всё
 * ещё переписка, у которой просто нет истории.
 *
 * @param lanes номера дорожек узлов
 * @return номер верхней дорожки, не больше нуля
 */
internal fun topLaneOf(lanes: List<Int>): Int {
  var top = 0
  lanes.forEach { lane ->
    if (lane < top) top = lane
  }
  return top
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
 * Смещения левых краёв узлов, накопленные по зазорам и ширинам.
 *
 * Зазор отделяет плашки, а не центры: плашка бывает шириной до 180 dp, и мерить сорок пикселей от
 * центра до центра значило бы положить узлы друг на друга. Отсюда же берётся длина ребра — она
 * равна зазору по построению.
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
 * Раскладка графа целиком: положения плашек, границы содержимого и связи.
 *
 * Чистая функция, и это не эстетика: вся арифметика раскладки, в которой случились все регрессии
 * этой фичи, здесь проверяется юнит-тестом, а не глазами на устройстве.
 *
 * Начало истории встаёт центром в центр экрана: слева от него отступ, а не обрезанная плашка. Сдвиг
 * привязан к первому узлу модели, а не к самому левому из размещённых, — иначе догрузка истории или
 * виртуализация уводили бы весь граф в сторону.
 *
 * @param lanes номер дорожки каждого узла
 * @param gaps зазор перед каждым узлом, в пикселях
 * @param laneYs смещение дорожки каждого узла по Y, в пикселях
 * @param sizes измеренные размеры узлов
 * @param viewportWidth ширина видимой области
 * @return раскладка, пустая при отсутствии узлов
 */
internal fun graphPlacementOf(
  lanes: List<Int>,
  gaps: List<Float>,
  laneYs: List<Float>,
  sizes: List<IntSize>,
  viewportWidth: Int
): GraphPlacement {
  if (sizes.isEmpty()) {
    return GraphPlacement.Empty
  }
  val widths = sizes.map { it.width.toFloat() }
  val lefts = leftOffsetsOf(gaps, widths)
  val leadingShift = viewportWidth / 2f - lefts.first() - widths.first() / 2f
  val nodes = sizes.mapIndexed { index, size ->
    IntOffset(
      x = (lefts[index] + leadingShift).roundToInt(),
      y = (laneYs[index] - size.height / 2f).roundToInt()
    )
  }
  return GraphPlacement(
    nodes = nodes,
    bounds = boundsOf(nodes, sizes),
    edges = edgesOf(lanes, laneYs, nodes, sizes)
  )
}

/**
 * Допустимый сдвиг содержимого по одной оси.
 *
 * Камера — это сдвиг содержимого: `экран = полотно + камера`. Чтобы начало содержимого встало у
 * начала экрана, нужен сдвиг `-min`; чтобы конец встал у конца экрана — `viewport - max`.
 *
 * Когда содержимое короче экрана, границы схлопываются в одно центрирующее значение: прижимать к
 * краю то, что помещается целиком, незачем. Когда содержимого нет вовсе, центрировать нечего и
 * сдвиг остаётся нулевым — иначе пустое полотно уезжало бы на пол-экрана.
 *
 * @param min начало содержимого в координатах полотна
 * @param max конец содержимого в координатах полотна
 * @param viewport размер видимой области по той же оси
 * @return диапазон сдвига, вырожденный в точку, когда содержимое помещается целиком
 */
internal fun panRangeOf(min: Float, max: Float, viewport: Float): ClosedFloatingPointRange<Float> {
  if (max <= min) {
    return 0f..0f
  }
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

/**
 * Объединение прямоугольников узлов.
 *
 * Единственный источник истины о протяжённости полотна — сами узлы. Объявленный извне размер
 * полотна был бы вторым, и они разошлись бы при первой же реальной переписке.
 *
 * @param nodes левые верхние углы узлов
 * @param sizes размеры узлов
 * @return объединение прямоугольников
 */
private fun boundsOf(nodes: List<IntOffset>, sizes: List<IntSize>): Rect {
  var left = Float.MAX_VALUE
  var top = Float.MAX_VALUE
  var right = -Float.MAX_VALUE
  var bottom = -Float.MAX_VALUE
  nodes.forEachIndexed { index, node ->
    val size = sizes[index]
    if (node.x < left) left = node.x.toFloat()
    if (node.y < top) top = node.y.toFloat()
    if (node.x + size.width > right) right = (node.x + size.width).toFloat()
    if (node.y + size.height > bottom) bottom = (node.y + size.height).toFloat()
  }
  return Rect(left, top, right, bottom)
}

/**
 * Отрезки связей: между соседними по времени узлами одной дорожки.
 *
 * Ребро существует только там, где есть что связывать, поэтому после последнего узла дорожки его
 * нет и линия не уходит в пустоту. Отрезок живёт строго в зазоре между плашками: узел бывает
 * полупрозрачным, и линия под ним просвечивала бы.
 *
 * Y берётся у дорожки, а не у плашки: плашки центрируются на дорожке с округлением, и у соседей
 * разной высоты центры расходились на пиксель — ребро не дотягивалось до одного из них.
 *
 * Дорожка сейчас отождествляется с ветвью. Когда появится переиспользование дорожки после слияния,
 * группировать придётся по идентификатору ветви, иначе две несвязанные ветви получат ложное ребро.
 *
 * @param lanes номер дорожки каждого узла
 * @param laneYs смещение дорожки каждого узла по Y
 * @param nodes левые верхние углы узлов
 * @param sizes размеры узлов
 * @return отрезки в координатах полотна
 */
private fun edgesOf(
  lanes: List<Int>,
  laneYs: List<Float>,
  nodes: List<IntOffset>,
  sizes: List<IntSize>
): List<GraphEdge> {
  val previousByLane = HashMap<Int, Int>()
  val edges = mutableListOf<GraphEdge>()
  nodes.indices.forEach { index ->
    val previous = previousByLane.put(lanes[index], index)
    if (previous != null) {
      val startX = (nodes[previous].x + sizes[previous].width).toFloat()
      val endX = nodes[index].x.toFloat()
      if (endX > startX) {
        edges += GraphEdge(startX = startX, endX = endX, y = laneYs[index])
      }
    }
  }
  return edges
}

private val LANE_STEP: Dp = 104.dp

internal val EDGE_WIDTH: Dp = 2.dp
