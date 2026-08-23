package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ru.sla.clarify.feature.chronology.ui.entity.GraphCameraRange
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.GraphPanStep
import ru.sla.clarify.feature.chronology.ui.entity.GraphPlacement
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
 * @return раскладка, пустая при отсутствии узлов
 */
internal fun graphPlacementOf(
  lanes: List<Int>,
  gaps: List<Float>,
  laneYs: List<Float>,
  sizes: List<IntSize>
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
  val centres = lefts.mapIndexed { index, left -> left + widths[index] / 2f }
  return GraphPlacement(
    nodes = nodes,
    bounds = boundsOf(nodes, sizes),
    edges = edgesOf(lanes, laneYs, nodes, sizes),
    // Минимум и максимум, а не первый с последним: агрегат не должен зависеть от того, что порядок
    // узлов совпадает с порядком по оси.
    centreSpanX = (centres.min())..(centres.max())
  )
}

/**
 * Допустимый сдвиг содержимого по оси времени.
 *
 * Камере разрешено наводиться на любой центр плашки и только на него: в покое в центре экрана стоит
 * самый левый узел, а докрутив вправо до упора — самый правый. Кламп по краям содержимого давал бы
 * обратное: начало истории у левой кромки, конец у правой.
 *
 * @param centreSpanX отрезок центров плашек в координатах полотна
 * @param viewport ширина видимой области
 * @return диапазон сдвига, вырожденный в точку при единственном узле
 */
internal fun timelinePanRangeOf(
  centreSpanX: ClosedFloatingPointRange<Float>,
  viewport: Float
): ClosedFloatingPointRange<Float> {
  val centre = viewport / 2f
  return (centre - centreSpanX.endInclusive)..(centre - centreSpanX.start)
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
 * Где камере разрешено быть при этой раскладке и этом вьюпорте.
 *
 * Собирает обе оси в одну величину, чтобы «где камере можно быть» имело единственное определение:
 * то же самое значение читается при показе, проверяется при записи и пере-накладывается после
 * раскладки. Пока определение жило выражением внутри чтения, запись о нём не знала.
 *
 * @param placement последняя раскладка графа
 * @param viewport размер видимой области
 * @return диапазоны по обеим осям; вырожденные — норма, а не краевой случай
 */
internal fun cameraRangeOf(placement: GraphPlacement, viewport: IntSize): GraphCameraRange {
  if (placement.isEmpty) {
    return GraphCameraRange.Empty
  }
  return GraphCameraRange(
    x = timelinePanRangeOf(placement.centreSpanX, viewport.width.toFloat()),
    y = panRangeOf(placement.bounds.top, placement.bounds.bottom, viewport.height.toFloat())
  )
}

/**
 * Двигает камеру на [delta], не выпуская её за [range].
 *
 * Кламп стоит на записи, а не на чтении, и это не перестановка мест. Накапливая незажатый сдвиг,
 * состояние банкует перерегулирование: упор в стенку на три тысячи пикселей превращается в мёртвую
 * зону такой же величины, которая сама не рассасывается — жест обратно сначала выбирает её и только
 * потом двигает картинку. Вылезает это не залипанием, а телепортом: следующая раскладка расширяет
 * диапазон, и камера за кадр уезжает на величину банка.
 *
 * @param camera текущий сдвиг содержимого
 * @param delta запрошенное приращение
 * @param range где камере разрешено быть
 * @return новый сдвиг и та часть [delta], которая в него уместилась
 */
internal fun panStepOf(camera: Offset, delta: Offset, range: GraphCameraRange): GraphPanStep {
  val moved = Offset(
    x = (camera.x + delta.x).coerceIn(range.x),
    y = (camera.y + delta.y).coerceIn(range.y)
  )
  return GraphPanStep(camera = moved, consumed = moved - camera)
}

/**
 * Упёрлась ли камера по всем осям, которые несут бросок.
 *
 * Признак смотрит на диапазон и на направление, а не на то, сколько взяла последняя дельта, и это
 * важнее, чем кажется. Первый кадр затухания приходит с нулевым приращением, и правило вида
 * «потребили меньше запрошенного» остановило бы бросок, не начав его. Оно же не умеет отличить
 * упор от оси, которая в броске просто не участвует.
 *
 * Условие `&&` — осознанный отход от платформы. Платформа гасит диагональ целиком, стоит упереться
 * одной оси; здесь ось Y вырождена, пока дорожка одна, поэтому любой слегка наклонный бросок умирал
 * бы мгновенно, и «fling на 360» работал бы ровно для одного угла. Пока бросок несёт хоть одна ось,
 * инерция живёт и едет вдоль стенки — как список, а не как стена.
 *
 * Цена решения: на вырожденной оси бросок проезжает только свою проекцию, но тратит на неё полную
 * длительность. Для пологих бросков это незаметно, для крутых — 80° дают 112 dp за те же 924 мс.
 * Если на устройстве это прочтётся как заедание, обратный ход — заменить `&&` на `||`.
 *
 * @param camera текущий сдвиг содержимого
 * @param direction единичный вектор броска, см. [FlingDirection]
 * @param range где камере разрешено быть
 * @return `true`, когда двигаться некуда и затухание пора обрывать
 */
internal fun isCameraStuck(camera: Offset, direction: Offset, range: GraphCameraRange): Boolean {
  return isAxisStuck(camera.x, direction.x, range.x) && isAxisStuck(camera.y, direction.y, range.y)
}

/**
 * Упёрлась ли одна ось.
 *
 * Нулевая компонента направления и вырожденный диапазон — одно и то же: нести бросок этой оси нечем.
 *
 * @param camera сдвиг по этой оси
 * @param direction компонента направления броска
 * @param range допустимый сдвиг по этой оси
 * @return `true`, когда по этой оси ехать некуда
 */
private fun isAxisStuck(
  camera: Float,
  direction: Float,
  range: ClosedFloatingPointRange<Float>
): Boolean {
  if (direction == 0f || range.start >= range.endInclusive) {
    return true
  }
  return if (direction > 0f) camera >= range.endInclusive else camera <= range.start
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
