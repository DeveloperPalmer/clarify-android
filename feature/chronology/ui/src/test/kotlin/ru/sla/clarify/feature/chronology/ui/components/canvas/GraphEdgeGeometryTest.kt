package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranchStatus
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdgeRole
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeRole
import ru.sla.clarify.feature.chronology.ui.entity.GraphVertical
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Сторожит то, ради чего рёбра перестали быть тремя числами.
 *
 * Пять проверок переехали сюда из `GraphPlacementTest` вместе с самими рёбрами: длина ребра равна
 * зазору, у последнего узла ветки нет ребра «в никуда», разные ветки не связываются. Остальные —
 * про то, чего у прежней модели не было вовсе: уход с магистрали, возврат к слиянию и хвост.
 *
 * Отдельно сторожится **группировка по ветке, а не по дорожке**: дорожка переиспользуется после
 * слияния, и по ней последний узел закрытой темы связался бы с первым узлом следующей — ложным
 * ребром через всю историю.
 */
class GraphEdgeGeometryTest {

  @Test
  fun `an edge is as long as the pause that made it`() {
    val edges = edgesOf(
      nodes = listOf(episode("1", "trunk"), episode("2", "trunk")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE)
    )

    val edge = edges.single()
    assertEquals(200f, edge.points.first().x, "ребро начинается у правого края первой плашки")
    assertEquals(340f, edge.points.last().x, "и кончается у левого края следующей")
  }

  @Test
  fun `the last node of a branch has no trailing edge`() {
    val edges = edgesOf(
      nodes = listOf(episode("1", "trunk"), episode("2", "trunk")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE)
    )

    assertEquals(1, edges.size, "магистраль не уходит в пустоту после последнего узла")
  }

  @Test
  fun `two branches on one lane are never joined by an edge`() {
    // Обе ветки стоят на одной дорожке: первая слита, вторая заняла её место. Ребро между ними
    // означало бы связь двух несвязанных тем через всю историю.
    val nodes = listOf(episode("1", "old"), episode("2", "new"))
    val edges = edgesOf(
      nodes = nodes,
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE),
      laneYs = listOf(100f, 100f),
      branches = listOf(
        branch("old", forkedFrom = null, mergedAt = null),
        branch("new", forkedFrom = null, mergedAt = null)
      )
    )

    assertTrue(
      edges.none { it.role == GraphEdgeRole.Branch },
      "ветки разные, и общая дорожка их не связывает"
    )
  }

  @Test
  fun `a fork route leaves the trunk at the fork centre`() {
    val edges = edgesOf(
      nodes = listOf(point("fork", GraphNodeRole.Fork), episode("1", "topic")),
      positions = listOf(IntOffset(0, 0), IntOffset(200, 0)),
      sizes = listOf(IntSize(24, 24), SIZE),
      laneYs = listOf(0f, 104f),
      branches = listOf(branch("topic", forkedFrom = "fork", mergedAt = null))
    )

    val fork = edges.single { it.role == GraphEdgeRole.Fork }
    assertEquals(
      listOf(Offset(12f, 0f), Offset(12f, 104f), Offset(200f, 104f)),
      fork.points,
      "уход идёт из центра точки ветвления вниз до дорожки и дальше до первой плашки"
    )
  }

  @Test
  fun `a merge route returns to the trunk at the merge centre`() {
    val edges = edgesOf(
      nodes = listOf(episode("1", "topic"), point("merge", GraphNodeRole.Merge)),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, IntSize(24, 24)),
      laneYs = listOf(104f, 0f),
      branches = listOf(branch("topic", forkedFrom = null, mergedAt = "merge"))
    )

    val merge = edges.single { it.role == GraphEdgeRole.Merge }
    assertEquals(
      listOf(Offset(200f, 104f), Offset(352f, 104f), Offset(352f, 0f)),
      merge.points,
      "возврат идёт от последней плашки до X слияния и там поднимается в магистраль"
    )
  }

  @Test
  fun `a branch without a merge ends in a tail`() {
    val edges = edgesOf(
      nodes = listOf(episode("1", "topic")),
      positions = listOf(IntOffset(0, 0)),
      sizes = listOf(SIZE),
      laneYs = listOf(104f),
      branches = listOf(branch("topic", forkedFrom = null, mergedAt = null)),
      contentRight = 900f
    )

    val tail = edges.single { it.role == GraphEdgeRole.Tail }
    assertEquals(
      listOf(Offset(200f, 104f), Offset(900f, 104f)),
      tail.points,
      "хвост живой темы тянется до правого края содержимого — §6.8"
    )
  }

  @Test
  fun `an abandoned branch has no tail`() {
    val edges = edgesOf(
      nodes = listOf(episode("1", "topic")),
      positions = listOf(IntOffset(0, 0)),
      sizes = listOf(SIZE),
      laneYs = listOf(104f),
      branches = listOf(
        branch("topic", forkedFrom = null, mergedAt = null, status = GraphBranchStatus.Abandoned)
      ),
      contentRight = 900f
    )

    assertTrue(
      edges.none { it.role == GraphEdgeRole.Tail },
      "тянуть линию брошенной темы через всю историю значило бы утверждать, что тема жива"
    )
  }

  @Test
  fun `a merged branch has no tail either`() {
    val edges = edgesOf(
      nodes = listOf(episode("1", "topic"), point("merge", GraphNodeRole.Merge)),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, IntSize(24, 24)),
      laneYs = listOf(104f, 0f),
      branches = listOf(branch("topic", forkedFrom = null, mergedAt = "merge")),
      contentRight = 900f
    )

    assertTrue(edges.none { it.role == GraphEdgeRole.Tail }, "закрытая тема кончается слиянием")
  }

  @Test
  fun `edges of the trunk and of a branch differ in role`() {
    val edges = edgesOf(
      nodes = listOf(episode("1", "trunk"), episode("2", "trunk")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE)
    )

    assertEquals(
      GraphEdgeRole.Trunk,
      edges.single().role,
      "у магистрали своя толщина, и роль — единственное, что её задаёт"
    )
  }
}

/**
 * Сторожит мостик над чужой дорожкой — §18.1 брифа, подтверждённый владельцем.
 *
 * Мостик нужен ровно там, где §4.2 обещал, что раскладки без пересечений хватит всегда: при трёх и
 * более одновременно живущих ветках вертикаль к внешней дорожке обязана пересечь горизонталь
 * внутренней.
 */
class GraphHopTest {

  @Test
  fun `a vertical crossing a horizontal raises a hop on it`() {
    val hops = hopsOf(
      verticals = listOf(GraphVertical(x = 500f, fromY = 0f, toY = 208f)),
      y = 104f,
      fromX = 0f,
      toX = 1000f,
      clearance = 16f
    )

    assertEquals(listOf(500f), hops)
  }

  @Test
  fun `a vertical outside the horizontal span raises nothing`() {
    val hops = hopsOf(
      verticals = listOf(GraphVertical(x = 1500f, fromY = 0f, toY = 208f)),
      y = 104f,
      fromX = 0f,
      toX = 1000f,
      clearance = 16f
    )

    assertTrue(hops.isEmpty(), "вертикаль за пределами отрезка его не пересекает")
  }

  @Test
  fun `a vertical ending on the horizontal raises nothing`() {
    val hops = hopsOf(
      verticals = listOf(GraphVertical(x = 500f, fromY = 0f, toY = 104f)),
      y = 104f,
      fromX = 0f,
      toX = 1000f,
      clearance = 16f
    )

    assertTrue(
      hops.isEmpty(),
      "вертикаль, кончающаяся на этой высоте, примыкает к линии, а не пересекает её: " +
        "это её собственный угол, и горб там встал бы на ровном месте"
    )
  }

  @Test
  fun `a hop closer than the clearance to either end is dropped`() {
    val hops = hopsOf(
      verticals = listOf(
        GraphVertical(x = 8f, fromY = 0f, toY = 208f),
        GraphVertical(x = 995f, fromY = 0f, toY = 208f)
      ),
      y = 104f,
      fromX = 0f,
      toX = 1000f,
      clearance = 16f
    )

    assertTrue(hops.isEmpty(), "прижатый к концу мостик съедается скруглением угла")
  }

  @Test
  fun `hops come sorted along the axis`() {
    val hops = hopsOf(
      verticals = listOf(
        GraphVertical(x = 700f, fromY = 0f, toY = 208f),
        GraphVertical(x = 300f, fromY = 208f, toY = 0f),
        GraphVertical(x = 500f, fromY = 0f, toY = 208f)
      ),
      y = 104f,
      fromX = 0f,
      toX = 1000f,
      clearance = 16f
    )

    assertEquals(
      listOf(300f, 500f, 700f),
      hops,
      "несортированные мостики заставили бы дугу вести путь назад: `arcTo` при `forceMoveTo = false` " +
        "молча вставит соединительную линию, а не разрыв"
    )
  }

  @Test
  fun `two branches produce no hops at all`() {
    // Дорожки ±1 соседствуют с магистралью: между ними нет ничего, что можно пересечь.
    val hops = hopsOf(
      verticals = listOf(GraphVertical(x = 500f, fromY = 0f, toY = 104f)),
      y = 104f,
      fromX = 0f,
      toX = 1000f,
      clearance = 16f
    )

    assertTrue(hops.isEmpty(), "мостик нужен начиная с третьей одновременно живущей ветки")
  }
}

private val SIZE = IntSize(width = 200, height = 72)

private fun episode(id: String, branchId: String): GraphNode {
  return GraphNode(
    id = GraphNode.Id(id),
    branchId = GraphBranch.Id(branchId),
    role = GraphNodeRole.Episode,
    gap = TimeGap.Hour
  )
}

private fun point(id: String, role: GraphNodeRole): GraphNode {
  return GraphNode(
    id = GraphNode.Id(id),
    branchId = GraphBranch.Id("trunk"),
    role = role,
    gap = TimeGap.Hour
  )
}

private fun branch(
  id: String,
  forkedFrom: String?,
  mergedAt: String?,
  status: GraphBranchStatus = GraphBranchStatus.Alive
): GraphBranch {
  return GraphBranch(
    id = GraphBranch.Id(id),
    colorIndex = 1,
    forkedFrom = forkedFrom?.let { GraphNode.Id(it) },
    mergedAt = mergedAt?.let { GraphNode.Id(it) },
    status = status
  )
}

@Suppress("LongParameterList")
private fun edgesOf(
  nodes: List<GraphNode>,
  positions: List<IntOffset>,
  sizes: List<IntSize>,
  laneYs: List<Float> = List(nodes.size) { 0f },
  branches: List<GraphBranch> = emptyList(),
  contentRight: Float = 0f
): List<ru.sla.clarify.feature.chronology.ui.entity.GraphEdge> {
  return graphEdgesOf(
    nodes = nodes,
    branches = branches,
    laneYs = laneYs,
    positions = positions,
    sizes = sizes,
    contentRight = contentRight,
    hopClearance = 16f
  )
}
