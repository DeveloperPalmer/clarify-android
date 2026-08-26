package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranchStatus
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdgeRole
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphVertical

/**
 * Все рёбра графа: горизонтали дорожек, уходы, возвраты, хвосты и мостики над чужими вертикалями.
 *
 * Три прохода, каждый линейный. Сначала горизонтали — между соседними по времени узлами **одной
 * ветки**, а не одной дорожки: дорожка переиспользуется после слияния (§4.2 брифа), и группировка по
 * ней связала бы последний узел закрытой темы с первым узлом следующей ложным ребром через всю
 * историю. Затем ветвления и возвраты, дающие вертикали. И только потом мостики — когда все
 * вертикали известны.
 *
 * @param nodes узлы в хронологическом порядке
 * @param branches ветки графа, кроме магистрали
 * @param laneYs смещение дорожки каждого узла по Y, в пикселях
 * @param positions левые верхние углы узлов
 * @param sizes измеренные размеры узлов
 * @param contentRight правый край содержимого: до него тянутся хвосты незакрытых тем
 * @param hopClearance наименьшее расстояние от мостика до конца отрезка
 * @return рёбра в координатах полотна
 */
@Suppress("LongParameterList")
internal fun graphEdgesOf(
  nodes: List<GraphNode>,
  branches: List<GraphBranch>,
  laneYs: List<Float>,
  positions: List<IntOffset>,
  sizes: List<IntSize>,
  contentRight: Float,
  hopClearance: Float
): List<GraphEdge> {
  if (nodes.isEmpty()) {
    return emptyList()
  }
  val indexById = nodes.withIndex().associate { (index, node) -> node.id to index }
  val colorOf = branches.associate { it.id to it.colorIndex }
  val statusOf = branches.associate { it.id to it.status }
  val verticals = mutableListOf<GraphVertical>()
  val edges = mutableListOf<GraphEdge>()

  edges += horizontalEdgesOf(nodes, laneYs, positions, sizes, colorOf, statusOf)
  edges += forkAndMergeEdgesOf(
    nodes = nodes,
    branches = branches,
    laneYs = laneYs,
    positions = positions,
    sizes = sizes,
    indexById = indexById,
    verticals = verticals
  )
  edges += tailEdgesOf(nodes, branches, laneYs, positions, sizes, contentRight)

  // Мостики ставятся последним проходом: пока не построены все вертикали, пересекать нечего.
  return edges.map { edge -> edge.copy(hops = hopsForEdgeOf(edge, verticals, hopClearance)) }
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
  edge: GraphEdge,
  verticals: List<GraphVertical>,
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
internal fun hopsOf(
  verticals: List<GraphVertical>,
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
  nodes: List<GraphNode>,
  laneYs: List<Float>,
  positions: List<IntOffset>,
  sizes: List<IntSize>,
  colorOf: Map<GraphBranch.Id, Int>,
  statusOf: Map<GraphBranch.Id, GraphBranchStatus>
): List<GraphEdge> {
  val previousByBranch = HashMap<GraphBranch.Id, Int>()
  val edges = mutableListOf<GraphEdge>()
  nodes.indices.forEach { index ->
    val branchId = nodes[index].branchId
    val previous = previousByBranch.put(branchId, index)
    if (previous != null) {
      val startX = (positions[previous].x + sizes[previous].width).toFloat()
      val endX = positions[index].x.toFloat()
      if (endX > startX) {
        val colorIndex = colorOf[branchId] ?: 0
        edges += GraphEdge(
          points = listOf(Offset(startX, laneYs[index]), Offset(endX, laneYs[index])),
          hops = emptyList(),
          colorIndex = colorIndex,
          role = if (colorIndex == 0) GraphEdgeRole.Trunk else GraphEdgeRole.Branch,
          status = statusOf[branchId] ?: GraphBranchStatus.Alive
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
  nodes: List<GraphNode>,
  branches: List<GraphBranch>,
  laneYs: List<Float>,
  positions: List<IntOffset>,
  sizes: List<IntSize>,
  indexById: Map<GraphNode.Id, Int>,
  verticals: MutableList<GraphVertical>
): List<GraphEdge> {
  val edges = mutableListOf<GraphEdge>()
  branches.forEach { branch ->
    val own = nodes.indices.filter { nodes[it].branchId == branch.id }
    val laneY = own.firstOrNull()?.let { laneYs[it] } ?: return@forEach
    val forkIndex = branch.forkedFrom?.let { indexById[it] }
    if (forkIndex != null) {
      val x = positions[forkIndex].x + sizes[forkIndex].width / 2f
      val trunkY = laneYs[forkIndex]
      val firstLeft = positions[own.first()].x.toFloat()
      verticals += GraphVertical(x = x, fromY = trunkY, toY = laneY)
      edges += GraphEdge(
        points = listOf(Offset(x, trunkY), Offset(x, laneY), Offset(maxOf(firstLeft, x), laneY)),
        hops = emptyList(),
        colorIndex = branch.colorIndex,
        role = GraphEdgeRole.Fork,
        status = branch.status
      )
    }
    val mergeIndex = branch.mergedAt?.let { indexById[it] }
    if (mergeIndex != null) {
      val x = positions[mergeIndex].x + sizes[mergeIndex].width / 2f
      val trunkY = laneYs[mergeIndex]
      val lastRight = (positions[own.last()].x + sizes[own.last()].width).toFloat()
      verticals += GraphVertical(x = x, fromY = laneY, toY = trunkY)
      edges += GraphEdge(
        points = listOf(Offset(minOf(lastRight, x), laneY), Offset(x, laneY), Offset(x, trunkY)),
        hops = emptyList(),
        colorIndex = branch.colorIndex,
        role = GraphEdgeRole.Merge,
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
  nodes: List<GraphNode>,
  branches: List<GraphBranch>,
  laneYs: List<Float>,
  positions: List<IntOffset>,
  sizes: List<IntSize>,
  contentRight: Float
): List<GraphEdge> {
  val edges = mutableListOf<GraphEdge>()
  branches.forEach { branch ->
    if (branch.mergedAt == null) {
      val own = nodes.indices.filter { nodes[it].branchId == branch.id }
      val last = own.lastOrNull() ?: return@forEach
      val startX = (positions[last].x + sizes[last].width).toFloat()
      if (contentRight > startX) {
        edges += GraphEdge(
          points = listOf(Offset(startX, laneYs[last]), Offset(contentRight, laneYs[last])),
          hops = emptyList(),
          colorIndex = branch.colorIndex,
          role = GraphEdgeRole.Tail,
          status = branch.status
        )
      }
    }
  }
  return edges
}
