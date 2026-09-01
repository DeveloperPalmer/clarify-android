package ru.sla.atlas.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import ru.sla.atlas.entity.BasicNode
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph

/**
 * Все рёбра графа: горизонтали дорожек, уходы, возвраты, хвосты и мостики над чужими вертикалями.
 *
 * Три прохода, каждый линейный. Сначала горизонтали — между соседними по времени узлами **одной
 * ветки**, а не одной дорожки: дорожка переиспользуется после слияния (§4.2 брифа), и группировка по
 * ней связала бы последний узел закрытой темы с первым узлом следующей ложным ребром через всю
 * историю. Затем ветвления и возвраты, дающие вертикали. И только потом мостики — когда все
 * вертикали известны.
 *
 * Порядок списка на выходе — это порядок отрисовки, то есть кто над кем лежит, и горбящиеся рёбра
 * поэтому уходят в его конец.
 *
 * @param graph граф: порядок узлов и состав веток
 * @param branchColors цвет каждой ветки графа, магистраль включая: ветки без цвета здесь быть не
 *   может, и её отсутствие — рассинхронизация наборов, а не значение по умолчанию
 * @param laneYs смещение дорожки каждого узла по Y, в пикселях
 * @param positions левые верхние углы узлов
 * @param sizes измеренные размеры узлов
 * @param contentRight правый край содержимого: до него тянутся хвосты незакрытых тем
 * @param hopClearance наименьшее расстояние от мостика до конца отрезка
 * @return рёбра в координатах полотна
 */
@Suppress("LongParameterList")
fun edgesOf(
  graph: Graph<BasicNode>,
  branchColors: Map<Branch.Id, Color>,
  laneYs: List<Float>,
  positions: List<IntOffset>,
  sizes: List<IntSize>,
  contentRight: Float,
  hopClearance: Float
): List<Edge> {
  if (graph.nodes.isEmpty()) {
    return emptyList()
  }
  val statusOf = graph.branches.associate { it.id to it.status }
  val verticals = mutableListOf<Vertical>()
  val edges = mutableListOf<Edge>()

  edges += horizontalEdgesOf(graph, branchColors, laneYs, positions, sizes, statusOf)
  edges += forkAndMergeEdgesOf(
    graph = graph,
    branchColors = branchColors,
    laneYs = laneYs,
    positions = positions,
    sizes = sizes,
    verticals = verticals
  )
  edges += tailEdgesOf(graph, branchColors, laneYs, positions, sizes, contentRight)

  // Мостики ставятся последним проходом: пока не построены все вертикали, пересекать нечего.
  return edges
    .map { edge -> edge.copy(hops = hopsForEdgeOf(edge, verticals, hopClearance)) }
    // Горбящееся ребро поднимается над всеми остальными, и в этом весь мостик: дуга обязана пройти
    // **над** чужой вертикалью. Вертикали строятся вторым проходом, то есть уже лежат поверх
    // горизонталей, и без подъёма чужая линия проходила бы сквозь горб. Сортировка устойчивая,
    // поэтому порядок внутри обеих половин остаётся тем, в котором рёбра построены.
    .sortedBy { edge -> edge.hops.isNotEmpty() }
}

/**
 * Мостики для одного ребра: над какими чужими вертикалями горбится каждый его горизонтальный
 * участок.
 *
 * Горбится **горизонталь**, а вертикаль идёт прямо. Правило сформулировано так, а не «внешняя
 * дорожка уступает внутренней», потому что не требует сравнивать номера дорожек и остаётся верным,
 * когда горизонталь принадлежит магистрали.
 *
 * @param edge ребро, чьи горизонтали проверяются
 * @param verticals все вертикали графа
 * @param clearance наименьшее расстояние от мостика до конца отрезка
 * @return координаты X в порядке возрастания
 */
private fun hopsForEdgeOf(
  edge: Edge,
  verticals: List<Vertical>,
  clearance: Float
): List<Float> {
  val hops = mutableListOf<Float>()
  edge.points.zipWithNext { from, to ->
    if (from.y == to.y) {
      hops += hopsOf(
        verticals = verticals,
        y = from.y,
        fromX = minOf(from.x, to.x),
        toX = maxOf(from.x, to.x),
        clearance = clearance
      )
    }
  }
  return hops.sorted()
}

/**
 * Координаты X вертикалей, пересекающих горизонтальный отрезок.
 *
 * Пересечение считается **строгим**: вертикаль, кончающаяся ровно на этой высоте, не пересекает
 * линию, а примыкает к ней — это её собственный угол, и мостик там означал бы горб на ровном месте.
 *
 * Мостик ближе `clearance` к концу отрезка отбрасывается: между углом маршрута и началом дуги должно
 * остаться место и на скругление, и на полухорду самого мостика, иначе скругление съедает дугу.
 *
 * @param verticals все вертикали графа
 * @param y высота горизонтального отрезка
 * @param fromX левый конец отрезка
 * @param toX правый конец отрезка
 * @param clearance наименьшее расстояние от мостика до конца отрезка
 * @return координаты X в порядке возрастания
 */
fun hopsOf(
  verticals: List<Vertical>,
  y: Float,
  fromX: Float,
  toX: Float,
  clearance: Float
): List<Float> {
  return verticals
    .filter { vertical ->
      val top = minOf(vertical.fromY, vertical.toY)
      val bottom = maxOf(vertical.fromY, vertical.toY)
      y > top && y < bottom && vertical.x > fromX + clearance && vertical.x < toX - clearance
    }
    .map { it.x }
    .sorted()
}

/**
 * Горизонтали внутри веток: между соседними по времени узлами одной ветки.
 *
 * Отрезок живёт строго в зазоре между плашками, и при нулевом зазоре ребра нет вовсе — рисовать его
 * негде. Y берётся у дорожки, а не у плашки: плашки центрируются с округлением, и у соседей разной
 * высоты центры расходились на пиксель.
 */
private fun horizontalEdgesOf(
  graph: Graph<BasicNode>,
  branchColors: Map<Branch.Id, Color>,
  laneYs: List<Float>,
  positions: List<IntOffset>,
  sizes: List<IntSize>,
  statusOf: Map<Branch.Id, Branch.Status>
): List<Edge> {
  val previousByBranch = HashMap<Branch.Id, Int>()
  val edges = mutableListOf<Edge>()
  graph.nodes.indices.forEach { index ->
    val branchId = graph.branchIds[index]
    val previous = previousByBranch.put(branchId, index)
    if (previous != null) {
      val startX = (positions[previous].x + sizes[previous].width).toFloat()
      val endX = positions[index].x.toFloat()
      if (endX > startX) {
        edges += Edge(
          points = listOf(Offset(startX, laneYs[index]), Offset(endX, laneYs[index])),
          hops = emptyList(),
          branchId = branchId,
          color = branchColors.getValue(branchId),
          // Магистраль опознаётся по тождеству ветки, а не по цвету: цвет повторяется каждые шесть
          // ответвлений, и седьмая ветка, взявшая нейтральный оттенок, стала бы магистралью.
          role = if (branchId == graph.baseline.id) EdgeRole.Baseline else EdgeRole.Branch,
          status = statusOf[branchId] ?: Branch.Status.Alive
        )
      }
    }
  }
  return edges
}

/**
 * Уходы с магистрали и возвраты в неё: Г-образные маршруты из трёх точек.
 *
 * Маршрут начинается в **центре** узла-точки, а не у его края: точка ветвления и точка слияния
 * залиты непрозрачно именно затем, чтобы прорезать линию собой.
 *
 * Порядок сегментов задан направлением и совпадает с прототипом: уход — сначала вертикаль, потом
 * горизонталь; возврат — сначала горизонталь, потом вертикаль. Из этого следует, что вертикаль
 * всегда стоит на X точки крепления, а угол — на высоте дорожки.
 *
 * Вертикали копятся в [verticals] по ходу дела: мостики ставятся отдельным проходом, когда известны
 * все.
 */
@Suppress("LongParameterList")
private fun forkAndMergeEdgesOf(
  graph: Graph<BasicNode>,
  branchColors: Map<Branch.Id, Color>,
  laneYs: List<Float>,
  positions: List<IntOffset>,
  sizes: List<IntSize>,
  verticals: MutableList<Vertical>
): List<Edge> {
  val indexById = graph.nodeIndexesById
  val edges = mutableListOf<Edge>()
  graph.branches.forEach { branch ->
    val own = branch.nodeIds.mapNotNull { indexById[it] }.sorted()
    val laneY = own.firstOrNull()?.let { laneYs[it] } ?: return@forEach
    val forkIndex = branch.forkedFrom?.let { indexById[it] }
    if (forkIndex != null) {
      val x = positions[forkIndex].x + sizes[forkIndex].width / 2f
      val baselineY = laneYs[forkIndex]
      val firstLeft = positions[own.first()].x.toFloat()
      verticals += Vertical(x = x, fromY = baselineY, toY = laneY)
      edges += Edge(
        points = listOf(Offset(x, baselineY), Offset(x, laneY), Offset(maxOf(firstLeft, x), laneY)),
        hops = emptyList(),
        branchId = branch.id,
        color = branchColors.getValue(branch.id),
        role = EdgeRole.Fork,
        status = branch.status
      )
    }
    val mergeIndex = branch.mergedAt?.let { indexById[it] }
    if (mergeIndex != null) {
      val x = positions[mergeIndex].x + sizes[mergeIndex].width / 2f
      val baselineY = laneYs[mergeIndex]
      val lastRight = (positions[own.last()].x + sizes[own.last()].width).toFloat()
      verticals += Vertical(x = x, fromY = laneY, toY = baselineY)
      edges += Edge(
        points = listOf(Offset(minOf(lastRight, x), laneY), Offset(x, laneY), Offset(x, baselineY)),
        hops = emptyList(),
        branchId = branch.id,
        color = branchColors.getValue(branch.id),
        role = EdgeRole.Merge,
        status = branch.status
      )
    }
  }
  return edges
}

/**
 * Хвосты незакрытых тем: от последнего узла ветки до правого края содержимого.
 *
 * Хвост есть у всего, что не слито: §6.8 брифа числит три состояния, и все три дают линию, уходящую
 * вправо, — «хвост тянется до правого края видимой области» это инвариант данных, а не декор. Тема
 * действительно ещё не закрыта, сколько бы она ни молчала: состояние «заброшена» отменено владельцем
 * (журнал, итерация 39) именно потому, что домен его не знает.
 *
 * Длину растворения задаёт рисование: раскладке довольно того, где хвост начинается и где кончается.
 */
private fun tailEdgesOf(
  graph: Graph<BasicNode>,
  branchColors: Map<Branch.Id, Color>,
  laneYs: List<Float>,
  positions: List<IntOffset>,
  sizes: List<IntSize>,
  contentRight: Float
): List<Edge> {
  val edges = mutableListOf<Edge>()
  graph.branches.forEach { branch ->
    if (branch.mergedAt == null) {
      val last = branch.nodeIds.mapNotNull { graph.nodeIndexesById[it] }.maxOrNull() ?: return@forEach
      val startX = (positions[last].x + sizes[last].width).toFloat()
      if (contentRight > startX) {
        edges += Edge(
          points = listOf(Offset(startX, laneYs[last]), Offset(contentRight, laneYs[last])),
          hops = emptyList(),
          branchId = branch.id,
          color = branchColors.getValue(branch.id),
          role = EdgeRole.Tail,
          status = branch.status
        )
      }
    }
  }
  return edges
}
