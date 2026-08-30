package ru.sla.clarify.feature.chronology.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.entity.BasicNode
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.TimeGap
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdgeRole
import ru.sla.clarify.feature.chronology.ui.entity.GraphVertical
import ru.sla.clarify.feature.chronology.ui.entity.Node

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
      nodes = listOf(episode("1"), episode("2")),
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
      nodes = listOf(episode("1"), episode("2")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE)
    )

    assertEquals(1, edges.size, "магистраль не уходит в пустоту после последнего узла")
  }

  @Test
  fun `two branches on one lane are never joined by an edge`() {
    // Обе ветки стоят на одной дорожке: первая слита, вторая заняла её место. Ребро между ними
    // означало бы связь двух несвязанных тем через всю историю.
    val nodes = listOf(episode("1"), episode("2"))
    val edges = edgesOf(
      nodes = nodes,
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE),
      laneYs = listOf(100f, 100f),
      branches = listOf(
        branch("old", forkedFrom = null, mergedAt = null, nodes = listOf("1")),
        branch("new", forkedFrom = null, mergedAt = null, nodes = listOf("2"))
      )
    )

    assertTrue(
      edges.none { it.role == GraphEdgeRole.Branch },
      "ветки разные, и общая дорожка их не связывает"
    )
  }

  @Test
  fun `a fork route leaves the baseline at the fork centre`() {
    val edges = edgesOf(
      nodes = listOf(fork("fork"), episode("1")),
      positions = listOf(IntOffset(0, 0), IntOffset(200, 0)),
      sizes = listOf(IntSize(24, 24), SIZE),
      laneYs = listOf(0f, 104f),
      branches = listOf(branch("topic", forkedFrom = "fork", mergedAt = null, nodes = listOf("1")))
    )

    val fork = edges.single { it.role == GraphEdgeRole.Fork }
    assertEquals(
      listOf(Offset(12f, 0f), Offset(12f, 104f), Offset(200f, 104f)),
      fork.points,
      "уход идёт из центра точки ветвления вниз до дорожки и дальше до первой плашки"
    )
  }

  @Test
  fun `a merge route returns to the baseline at the merge centre`() {
    val edges = edgesOf(
      nodes = listOf(episode("1"), merge("merge")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, IntSize(24, 24)),
      laneYs = listOf(104f, 0f),
      branches = listOf(branch("topic", forkedFrom = null, mergedAt = "merge", nodes = listOf("1")))
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
      nodes = listOf(episode("1")),
      positions = listOf(IntOffset(0, 0)),
      sizes = listOf(SIZE),
      laneYs = listOf(104f),
      branches = listOf(branch("topic", forkedFrom = null, mergedAt = null, nodes = listOf("1"))),
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
  fun `a merged branch has no tail`() {
    val edges = edgesOf(
      nodes = listOf(episode("1"), merge("merge")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, IntSize(24, 24)),
      laneYs = listOf(104f, 0f),
      branches = listOf(branch("topic", forkedFrom = null, mergedAt = "merge", nodes = listOf("1"))),
      contentRight = 900f
    )

    assertTrue(edges.none { it.role == GraphEdgeRole.Tail }, "закрытая тема кончается слиянием")
  }

  @Test
  fun `edges of the baseline and of a branch differ in role`() {
    val edges = edgesOf(
      nodes = listOf(episode("1"), episode("2")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE)
    )

    assertEquals(
      GraphEdgeRole.Baseline,
      edges.single().role,
      "у магистрали своя толщина, и роль — единственное, что её задаёт"
    )
  }

  @Test
  fun `every edge of a branch carries that branch's id`() {
    val edges = edgesOf(
      nodes = listOf(episode("1"), episode("2"), episode("3")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0), IntOffset(680, 0)),
      sizes = listOf(SIZE, SIZE, SIZE),
      laneYs = listOf(0f, 104f, 104f),
      branches = listOf(branch("a", forkedFrom = "1", mergedAt = null, nodes = listOf("2", "3")))
    )

    assertEquals(
      setOf(Branch.Id("a")),
      edges.map { it.branchId }.toSet(),
      "и уход с магистрали, и горизонталь дорожки принадлежат самой ветке"
    )
  }

  @Test
  fun `two branches of one shade stay apart by id`() {
    val edges = edgesOf(
      nodes = listOf(episode("1"), episode("2"), episode("3"), episode("4")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0), IntOffset(680, 0), IntOffset(1020, 0)),
      sizes = listOf(SIZE, SIZE, SIZE, SIZE),
      laneYs = listOf(104f, 104f, 208f, 208f),
      branches = listOf(
        branch("a", forkedFrom = null, mergedAt = null, nodes = listOf("1", "2")),
        branch("b", forkedFrom = null, mergedAt = null, nodes = listOf("3", "4"))
      )
    )

    assertEquals(
      1,
      edges.map { it.color }.toSet().size,
      "оттенок у обеих веток один: цвет повторяется каждые шесть ответвлений"
    )
    assertEquals(
      2,
      edges.map { it.branchId }.toSet().size,
      "а идентификатор их различает — по цвету церемония позолотила бы обе линии"
    )
  }

  @Test
  fun `an edge of the baseline carries the root branch id`() {
    val edges = edgesOf(
      nodes = listOf(episode("1"), episode("2")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE)
    )

    assertEquals(
      Branch.Id("baseline"),
      edges.single().branchId,
      "у магистрали идентификатор корневой ветки, а не пустой"
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

  @Test
  fun `an edge with a hop is drawn after the vertical it hops over`() {
    // Три дорожки: магистраль, ветка `b` на средней и ветка `c` на внешней. Уход `c` пересекает
    // горизонталь `b`, и та горбится.
    val edges = edgesOf(
      nodes = listOf(
        episode("1"),
        episode("2"),
        episode("3"),
        episode("4")
      ),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0), IntOffset(680, 0), IntOffset(1020, 0)),
      sizes = listOf(SIZE, SIZE, SIZE, SIZE),
      laneYs = listOf(104f, 0f, 104f, 208f),
      branches = listOf(
        branch("b", forkedFrom = null, mergedAt = null, nodes = listOf("1", "3")),
        branch("c", forkedFrom = "2", mergedAt = null, nodes = listOf("4"))
      )
    )

    val hopped = edges.indexOfFirst { it.hops.isNotEmpty() }
    assertTrue(hopped >= 0, "горизонталь средней дорожки пересечена уходом внешней")
    assertEquals(
      edges.lastIndex,
      hopped,
      "порядок списка — порядок отрисовки: горб, нарисованный раньше вертикали, ляжет под ней, " +
        "и мостик перестанет быть мостиком"
    )
  }
}

private val SIZE = IntSize(width = 200, height = 72)

private fun episode(id: String): Node.Episode {
  return Node.Episode(
    id = BasicNode.Id(id),
    gap = TimeGap.Hour,
    time = "6 мар, 10:00",
    count = 1,
    snippet = "",
    myShare = 0f,
    unreadCount = 0,
    dim = false
  )
}

private fun fork(id: String): Node.Fork {
  return Node.Fork(id = BasicNode.Id(id), gap = TimeGap.Hour)
}

private fun merge(id: String): Node.Merge {
  return Node.Merge(id = BasicNode.Id(id), gap = TimeGap.Hour)
}

/**
 * Ветка с её составом: какие узлы истории ей принадлежат.
 *
 * @param nodes идентификаторы собственных узлов ветки; остальные достаются магистрали
 */
private fun branch(
  id: String,
  forkedFrom: String?,
  mergedAt: String?,
  nodes: List<String> = emptyList(),
  status: Branch.Status = Branch.Status.Alive
): Branch {
  return Branch(
    id = Branch.Id(id),
    nodeIds = nodes.map { BasicNode.Id(it) },
    colorIndex = 1,
    forkedFrom = forkedFrom?.let { BasicNode.Id(it) },
    mergedAt = mergedAt?.let { BasicNode.Id(it) },
    status = status
  )
}

@Suppress("LongParameterList")
private fun edgesOf(
  nodes: List<Node>,
  positions: List<IntOffset>,
  sizes: List<IntSize>,
  laneYs: List<Float> = List(nodes.size) { 0f },
  branches: List<Branch> = emptyList(),
  contentRight: Float = 0f
): List<ru.sla.clarify.feature.chronology.ui.entity.GraphEdge> {
  val owned = branches.flatMap { it.nodeIds }.toSet()
  val graph = Graph(
    nodes = nodes,
    // Всё, что ветки не разобрали, стоит на магистрали — как и в настоящем графе.
    baseline = Branch(
      id = Branch.Id("baseline"),
      nodeIds = nodes.map { it.id }.filterNot { it in owned },
      colorIndex = 0,
      forkedFrom = null,
      mergedAt = null,
      status = Branch.Status.Alive
    ),
    branches = branches
  )
  return graphEdgesOf(
    graph = graph,
    branchColors = graph.mockBranchColors(),
    laneYs = laneYs,
    positions = positions,
    sizes = sizes,
    contentRight = contentRight,
    hopClearance = 16f
  )
}
