package ru.sla.atlas.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.entity.BasicNode
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.mockBranch
import ru.sla.atlas.entity.mockBranchColors
import ru.sla.atlas.entity.mockNode

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
 *
 * Узлы здесь одного рода на всю проверку, и это не упрощение: маршрут выводится из состава ветки и
 * её ориентиров, а из самого узла — только его место и размер. Пока узлы были трёх родов, тест
 * подсказывал обратное — будто уход с магистрали рисуется потому, что в этом месте стоит развилка.
 */
class GraphEdgeGeometryTest {

  @Test
  fun `an edge is as long as the pause that made it`() {
    val edges = mockEdgesOf(
      nodes = listOf(mockNode("1"), mockNode("2")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE)
    )

    val edge = edges.single()
    assertEquals(200f, edge.points.first().x, "ребро начинается у правого края первой плашки")
    assertEquals(340f, edge.points.last().x, "и кончается у левого края следующей")
  }

  @Test
  fun `the last node of a branch has no trailing edge`() {
    val edges = mockEdgesOf(
      nodes = listOf(mockNode("1"), mockNode("2")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE)
    )

    assertEquals(1, edges.size, "магистраль не уходит в пустоту после последнего узла")
  }

  @Test
  fun `two branches on one lane are never joined by an edge`() {
    // Обе ветки стоят на одной дорожке: первая слита, вторая заняла её место. Ребро между ними
    // означало бы связь двух несвязанных тем через всю историю.
    val nodes = listOf(mockNode("1"), mockNode("2"))
    val edges = mockEdgesOf(
      nodes = nodes,
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE),
      laneYs = listOf(100f, 100f),
      branches = listOf(
        mockBranch("old", forkedFrom = null, mergedAt = null, nodes = listOf("1")),
        mockBranch("new", forkedFrom = null, mergedAt = null, nodes = listOf("2"))
      )
    )

    assertTrue(
      edges.none { it.role == EdgeRole.Branch },
      "ветки разные, и общая дорожка их не связывает"
    )
  }

  @Test
  fun `a fork route leaves the baseline at the fork centre`() {
    val edges = mockEdgesOf(
      nodes = listOf(mockNode("fork"), mockNode("1")),
      positions = listOf(IntOffset(0, 0), IntOffset(200, 0)),
      sizes = listOf(IntSize(24, 24), SIZE),
      laneYs = listOf(0f, 104f),
      branches = listOf(mockBranch("topic", forkedFrom = "fork", mergedAt = null, nodes = listOf("1")))
    )

    val fork = edges.single { it.role == EdgeRole.Fork }
    assertEquals(
      listOf(Offset(12f, 0f), Offset(12f, 104f), Offset(200f, 104f)),
      fork.points,
      "уход идёт из центра точки ветвления вниз до дорожки и дальше до первой плашки"
    )
  }

  @Test
  fun `a merge route returns to the baseline at the merge centre`() {
    val edges = mockEdgesOf(
      nodes = listOf(mockNode("1"), mockNode("merge")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, IntSize(24, 24)),
      laneYs = listOf(104f, 0f),
      branches = listOf(mockBranch("topic", forkedFrom = null, mergedAt = "merge", nodes = listOf("1")))
    )

    val merge = edges.single { it.role == EdgeRole.Merge }
    assertEquals(
      listOf(Offset(200f, 104f), Offset(352f, 104f), Offset(352f, 0f)),
      merge.points,
      "возврат идёт от последней плашки до X слияния и там поднимается в магистраль"
    )
  }

  @Test
  fun `a branch without a merge ends in a tail`() {
    val edges = mockEdgesOf(
      nodes = listOf(mockNode("1")),
      positions = listOf(IntOffset(0, 0)),
      sizes = listOf(SIZE),
      laneYs = listOf(104f),
      branches = listOf(mockBranch("topic", forkedFrom = null, mergedAt = null, nodes = listOf("1"))),
      contentRight = 900f
    )

    val tail = edges.single { it.role == EdgeRole.Tail }
    assertEquals(
      listOf(Offset(200f, 104f), Offset(900f, 104f)),
      tail.points,
      "хвост живой темы тянется до правого края содержимого — §6.8"
    )
  }

  @Test
  fun `a merged branch has no tail`() {
    val edges = mockEdgesOf(
      nodes = listOf(mockNode("1"), mockNode("merge")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, IntSize(24, 24)),
      laneYs = listOf(104f, 0f),
      branches = listOf(mockBranch("topic", forkedFrom = null, mergedAt = "merge", nodes = listOf("1"))),
      contentRight = 900f
    )

    assertTrue(edges.none { it.role == EdgeRole.Tail }, "закрытая тема кончается слиянием")
  }

  @Test
  fun `edges of the baseline and of a branch differ in role`() {
    val edges = mockEdgesOf(
      nodes = listOf(mockNode("1"), mockNode("2")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0)),
      sizes = listOf(SIZE, SIZE)
    )

    assertEquals(
      EdgeRole.Baseline,
      edges.single().role,
      "у магистрали своя толщина, и роль — единственное, что её задаёт"
    )
  }

  @Test
  fun `every edge of a branch carries that branch's id`() {
    val edges = mockEdgesOf(
      nodes = listOf(mockNode("1"), mockNode("2"), mockNode("3")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0), IntOffset(680, 0)),
      sizes = listOf(SIZE, SIZE, SIZE),
      laneYs = listOf(0f, 104f, 104f),
      branches = listOf(mockBranch("a", forkedFrom = "1", mergedAt = null, nodes = listOf("2", "3")))
    )

    assertEquals(
      setOf(Branch.Id("a")),
      edges.map { it.branchId }.toSet(),
      "и уход с магистрали, и горизонталь дорожки принадлежат самой ветке"
    )
  }

  @Test
  fun `two branches of one shade stay apart by id`() {
    val edges = mockEdgesOf(
      nodes = listOf(mockNode("1"), mockNode("2"), mockNode("3"), mockNode("4")),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0), IntOffset(680, 0), IntOffset(1020, 0)),
      sizes = listOf(SIZE, SIZE, SIZE, SIZE),
      laneYs = listOf(104f, 104f, 208f, 208f),
      branches = listOf(
        mockBranch("a", forkedFrom = null, mergedAt = null, nodes = listOf("1", "2")),
        mockBranch("b", forkedFrom = null, mergedAt = null, nodes = listOf("3", "4"))
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
    val edges = mockEdgesOf(
      nodes = listOf(mockNode("1"), mockNode("2")),
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
      verticals = listOf(Vertical(x = 500f, fromY = 0f, toY = 208f)),
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
      verticals = listOf(Vertical(x = 1500f, fromY = 0f, toY = 208f)),
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
      verticals = listOf(Vertical(x = 500f, fromY = 0f, toY = 104f)),
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
        Vertical(x = 8f, fromY = 0f, toY = 208f),
        Vertical(x = 995f, fromY = 0f, toY = 208f)
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
        Vertical(x = 700f, fromY = 0f, toY = 208f),
        Vertical(x = 300f, fromY = 208f, toY = 0f),
        Vertical(x = 500f, fromY = 0f, toY = 208f)
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
      verticals = listOf(Vertical(x = 500f, fromY = 0f, toY = 104f)),
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
    val edges = mockEdgesOf(
      nodes = listOf(
        mockNode("1"),
        mockNode("2"),
        mockNode("3"),
        mockNode("4")
      ),
      positions = listOf(IntOffset(0, 0), IntOffset(340, 0), IntOffset(680, 0), IntOffset(1020, 0)),
      sizes = listOf(SIZE, SIZE, SIZE, SIZE),
      laneYs = listOf(104f, 0f, 104f, 208f),
      branches = listOf(
        mockBranch("b", forkedFrom = null, mergedAt = null, nodes = listOf("1", "3")),
        mockBranch("c", forkedFrom = "2", mergedAt = null, nodes = listOf("4"))
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

@Suppress("LongParameterList")
private fun mockEdgesOf(
  nodes: List<BasicNode>,
  positions: List<IntOffset>,
  sizes: List<IntSize>,
  laneYs: List<Float> = List(nodes.size) { 0f },
  branches: List<Branch> = emptyList(),
  contentRight: Float = 0f
): List<Edge> {
  val owned = branches.flatMap { it.nodeIds }.toSet()
  val graph = Graph(
    nodes = nodes,
    // Всё, что ветки не разобрали, стоит на магистрали — как и в настоящем графе.
    baseline = mockBranch(
      id = "baseline",
      nodes = nodes.map { it.id.value }.filterNot { owned.contains(BasicNode.Id(it)) },
      colorIndex = 0
    ),
    branches = branches
  )
  return edgesOf(
    graph = graph,
    branchColors = graph.mockBranchColors(),
    laneYs = laneYs,
    positions = positions,
    sizes = sizes,
    contentRight = contentRight,
    hopClearance = 16f
  )
}
