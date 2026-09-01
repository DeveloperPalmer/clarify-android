package ru.sla.atlas.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Где на полотне лежат дорожки, если самая верхняя из занятых — [topLane].
 *
 * Только пространство: ни состояния, ни времени, ни плотности экрана, ни Compose. Конфигурация —
 * два числа, а не список узлов, именно поэтому обращение к дорожке стоит O(1): раньше здесь
 * пересчитывался диапазон дорожек на каждый вызов, и обход узлов выходил квадратичным.
 *
 * Координаты отсчитываются от левого верхнего угла полотна. Начало полотна — не начало экрана:
 * сдвиг под вьюпорт добавляет камера, и геометрия о нём не знает.
 *
 * Шаг дорожки приходит параметром, а не берётся константой файла: он свойство уровня детализации —
 * уровень детализации сжимает раскладку по обеим осям одним и тем же числом, и шаг задаёт
 * вызывающий. Из-за этого же тип перестал быть `value class`: одно поле второго не вмещает.
 *
 * @param topLane номер самой верхней занятой дорожки, см. [topLaneOf]
 * @param laneStep расстояние между соседними дорожками на этом уровне детализации
 */
class LaneGeometry(private val topLane: Int, private val laneStep: Dp) {

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
    return laneStep * (lane - topLane) + laneStep / 2
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
fun topLaneOf(lanes: List<Int>): Int {
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
fun leftOffsetsOf(gaps: List<Float>, widths: List<Float>): List<Float> {
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
fun nearestCentreIndexOf(centres: List<Offset>, x: Float): Int {
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
 * Чистая функция, и это не эстетика: вся арифметика раскладки проверяется здесь юнит-тестом, а не
 * глазами на устройстве. Регрессии раскладки случались именно в ней.
 *
 * Полотно начинается в нуле: центрировать начало истории здесь незачем и вредно — кламп камеры
 * прижимал бы содержимое к краю экрана и ровно отменял такое смещение. За то, куда камера
 * наводится, отвечает сама камера.
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
fun placementOf(
  lanes: List<Int>,
  gaps: List<Float>,
  laneYs: List<Float>,
  sizes: List<IntSize>,
  margins: CanvasMargins
): Placement {
  check(lanes.size == gaps.size && lanes.size == laneYs.size && lanes.size == sizes.size) {
    "Раскладка получила рассогласованные списки: " +
      "lanes=${lanes.size}, " +
      "gaps=${gaps.size}, " +
      "laneYs=${laneYs.size}, " +
      "sizes=${sizes.size}"
  }
  if (sizes.isEmpty()) {
    return Placement.Empty
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
  return Placement(
    nodes = nodes,
    sizes = sizes,
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
 * Где прямоугольник полотна оказывается на экране.
 *
 * Та же формула, на которой построена вся камера, — `экран = полотно · scale + камера`. Второй её
 * записи в фиче нет и быть не должно: раскладка живёт в координатах полотна, а всё, что кладётся
 * поверх слоя камеры — якорь морфа прежде всего, — обязано попадать в плашку пиксель в пиксель.
 *
 * Спрашивать у самого узла его `LayoutCoordinates` было бы короче, но тогда это перестало бы быть
 * арифметикой: проверить «на 2.5× плашка вдвое с половиной больше» можно только здесь, без Compose.
 *
 * @param topLeft левый верхний угол узла в координатах полотна
 * @param size измеренный размер узла
 * @param camera сдвиг содержимого относительно экрана
 * @param scale масштаб камеры
 * @return прямоугольник узла в координатах вьюпорта, уже с учётом масштаба
 */
fun screenRectOf(topLeft: IntOffset, size: IntSize, camera: Offset, scale: Float): Rect {
  return Rect(
    offset = Offset(x = topLeft.x * scale + camera.x, y = topLeft.y * scale + camera.y),
    size = Size(width = size.width * scale, height = size.height * scale)
  )
}

/**
 * Прямоугольник, раздутый полями.
 *
 * @param margins поля по четырём сторонам
 * @return прямоугольник, включающий поля
 */
private fun Rect.expandedBy(margins: CanvasMargins): Rect {
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
