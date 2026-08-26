package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.GraphCanvasMargins
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement
import kotlin.math.abs
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
 * Смещения левых краёв узлов, накопленные по зазорам и ширинам.
 *
 * Зазор отделяет плашки, а не центры: эпизод шириной 200 dp, сообщение — до 180 dp, и мерить сорок
 * пикселей от центра до центра значило бы положить узлы друг на друга. Отсюда же берётся длина
 * ребра — она равна зазору по построению.
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
 * Индекс плашки, ближайшей к [x] по времени.
 *
 * Перебором, а не двоичным поиском: [centres] упорядочены по времени, а не по оси, и совпадение
 * этих порядков — то самое допущение, из-за которого агрегаты раскладки считаются через `min` и
 * `max`. Двадцать два узла демо-набора стоят ничего; когда появится виртуализация и узлов станут
 * тысячи, обратный ход — держать рядом отсортированный массив координат и искать по нему.
 *
 * @param centres центры плашек в координатах полотна
 * @param x координата полотна по оси времени
 * @return индекс ближайшей плашки; `-1`, когда плашек нет
 */
internal fun nearestCentreIndexOf(centres: List<Offset>, x: Float): Int {
  var nearest = -1
  var distance = Float.MAX_VALUE
  centres.forEachIndexed { index, centre ->
    val candidate = abs(centre.x - x)
    if (candidate < distance) {
      distance = candidate
      nearest = index
    }
  }
  return nearest
}

/**
 * Раскладка графа целиком: положения плашек, границы содержимого и связи.
 *
 * Чистая функция, и это не эстетика: вся арифметика раскладки, в которой случились все регрессии
 * этой фичи, здесь проверяется юнит-тестом, а не глазами на устройстве.
 *
 * Полотно начинается в нуле: центрировать начало истории здесь незачем и вредно — кламп камеры
 * прижимал бы содержимое к краю экрана и ровно отменял такое смещение. За то, куда камера наводится,
 * отвечает [timelinePanRangeOf].
 *
 * Все четыре списка обязаны быть одной длины, и это проверяется, а не подразумевается: три из них
 * описывают модель, четвёртый приходит из фазы измерения, и рассинхрон между этими источниками —
 * не «невозможное состояние», а ровно тот дефект, который здесь и ловится. Индекс за границей
 * списка сообщил бы о нём в терминах реализации, а не в терминах нарушенного контракта.
 *
 * @param lanes номер дорожки каждого узла
 * @param gaps зазор перед каждым узлом, в пикселях
 * @param laneYs смещение дорожки каждого узла по Y, в пикселях
 * @param sizes измеренные размеры узлов
 * @param margins поля полотна вокруг содержимого
 * @return раскладка, пустая при отсутствии узлов
 */
internal fun graphPlacementOf(
  lanes: List<Int>,
  gaps: List<Float>,
  laneYs: List<Float>,
  sizes: List<IntSize>,
  margins: GraphCanvasMargins
): GraphPlacement {
  check(lanes.size == gaps.size && lanes.size == laneYs.size && lanes.size == sizes.size) {
    "Раскладка получила рассогласованные списки: " +
      "lanes=${lanes.size}, " +
      "gaps=${gaps.size}, " +
      "laneYs=${laneYs.size}, " +
      "sizes=${sizes.size}"
  }
  if (sizes.isEmpty()) {
    return GraphPlacement.Empty
  }
  val widths = sizes.map { it.width.toFloat() }
  val lefts = leftOffsetsOf(gaps, widths)
  val nodes = sizes.mapIndexed { index, size ->
    IntOffset(
      x = lefts[index].roundToInt(),
      y = (laneYs[index] - size.height / 2f).roundToInt()
    )
  }
  val centresX = lefts.mapIndexed { index, left -> left + widths[index] / 2f }
  val centres = centresX.mapIndexed { index, centreX -> Offset(x = centreX, y = laneYs[index]) }
  return GraphPlacement(
    nodes = nodes,
    // Поля входят в протяжённость полотна, а не добавляются камере отдельным слагаемым: диапазон
    // выводится из bounds, и раздутый прямоугольник сам даёт зазор у каждой границы. Иначе крайняя
    // плашка упирается в кромку экрана, будто история обрезана.
    bounds = boundsOf(nodes, sizes).expandedBy(margins),
    // Минимум и максимум, а не первый с последним: агрегат не должен зависеть от того, что порядок
    // узлов совпадает с порядком по оси. По Y это уже не педантизм, а необходимость — порядок узлов
    // задан временем, и с порядком дорожек не совпадает вовсе.
    // Отрезки держатся готовыми, а не выводятся из centres по требованию: диапазон камеры
    // спрашивают на каждом кадре жеста, и проход по всем узлам на кадр — это ровно тот обход,
    // которого здесь избегают.
    centreSpanX = (centresX.min())..(centresX.max()),
    centreSpanY = (laneYs.min())..(laneYs.max()),
    centres = centres
  )
}

/**
 * Поля полотна: базовый отступ со всех сторон плюс системные врезки сверху и снизу.
 *
 * Полотно занимает весь экран под системными барами — иначе панорамирование обрывалось бы там, где
 * начинается статус-бар, а фон не доходил бы до кромки. Значит уводить плашки из-под баров должны
 * поля, а не размер полотна.
 *
 * @param base отступ, одинаковый со всех сторон
 * @param statusBar высота строки состояния
 * @param navigationBar высота навигационной полосы
 * @return поля по четырём сторонам
 */
internal fun canvasMarginsOf(base: Float, statusBar: Float, navigationBar: Float): GraphCanvasMargins {
  return GraphCanvasMargins(
    left = base,
    top = base + statusBar,
    right = base,
    bottom = base + navigationBar
  )
}

/**
 * Прямоугольник, раздутый полями.
 *
 * @param margins поля по четырём сторонам
 * @return прямоугольник, включающий поля
 */
private fun Rect.expandedBy(margins: GraphCanvasMargins): Rect {
  return Rect(
    left = left - margins.left,
    top = top - margins.top,
    right = right + margins.right,
    bottom = bottom + margins.bottom
  )
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

internal val CANVAS_PADDING: Dp = 64.dp
private val LANE_STEP: Dp = 104.dp
