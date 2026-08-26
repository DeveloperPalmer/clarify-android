package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.clarify.feature.chronology.ui.components.canvas.graphEdgesOf
import ru.sla.clarify.feature.chronology.ui.components.canvas.graphLanesOf
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranch
import ru.sla.clarify.feature.chronology.ui.entity.GraphBranchStatus
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdgeRole
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeRole
import ru.sla.clarify.feature.chronology.ui.entity.TimeGap

/**
 * Сторожит **сценарии**, ради которых демо-наборы и собраны, а не их содержимое.
 *
 * Набор — единственное, на чём фича проверяется глазами, и каждая его ветка отвечает за состояние,
 * которое рисуется по-своему. Убрав одну, легко не заметить, что вместе с ней с экрана пропала целая
 * ветвь поведения: прежний набор два десятка итераций притворялся графом, не имея ни одной настоящей
 * ветки, и вскрылось это только когда мини-карта показала его в другой проекции.
 *
 * Поэтому тест проверяет не «сколько узлов», а «остались ли в наборе те положения, из-за которых
 * раскладка однажды сломалась».
 */
class MocksTest {

  @Test
  fun `a branch forked between a neighbour's last node and its merge keeps its own lane`() {
    assertTrue(
      demoLanes()[GraphBranch.Id("terms")] != demoLanes()[GraphBranch.Id("design")],
      "ветка design уходит с магистрали до слияния terms, и делить дорожку им нельзя: " +
        "горизонталь возврата terms прошла бы сквозь плашки design"
    )
  }

  @Test
  fun `a merged branch hands its lane over to a later one`() {
    val lanes = demoLanes()

    assertEquals(
      lanes[GraphBranch.Id("terms")],
      lanes[GraphBranch.Id("budget")],
      "terms слита раньше, чем budget ответвилась, и дорожка обязана переиспользоваться — §4.2"
    )
    assertEquals(
      lanes[GraphBranch.Id("photos")],
      lanes[GraphBranch.Id("release")],
      "второе слияние обязано отдавать дорожку так же, как первое: одного примера в наборе мало, " +
        "потому что он не отличает правило от совпадения"
    )
  }

  @Test
  fun `an abandoned branch hands its lane over to a later one`() {
    assertEquals(
      demoLanes()[GraphBranch.Id("logo")],
      demoLanes()[GraphBranch.Id("pricing")],
      "дорожку освобождает не только слияние: брошенная тема отдаёт её тоже, иначе три брошенных " +
        "темы держали бы три дорожки до конца истории — §6.8"
    )
  }

  @Test
  fun `a branch without a fork anchor gets a lane of its own`() {
    val branches = mockGraph().branches
    val lanes = demoLanes()
    val anchorless = branches.single { it.forkedFrom == null }

    assertTrue(
      lanes[anchorless.id] != null && lanes[anchorless.id] != 0,
      "ветка с удалённым сообщением-развилкой (§15 п. 10) обязана получить дорожку: занятость " +
        "считается от первого своего узла, а не теряется вместе с якорем"
    )
    assertTrue(
      lanes[anchorless.id] !in branches.filter { it.id != anchorless.id }.mapNotNull { lanes[it.id] },
      "дорожка ветки без якоря не может совпадать с чужой: все прочие ветки набора к этому моменту " +
        "ещё живы"
    )
  }

  @Test
  fun `two branches leave the same fork node`() {
    val branches = mockGraph().branches
    val shared = branches
      .mapNotNull { it.forkedFrom }
      .groupingBy { it }
      .eachCount()
      .filterValues { it > 1 }
      .keys

    assertTrue(
      shared.isNotEmpty(),
      "от одного коммита может уйти несколько веток, и акцент точки достаётся первой из них: " +
        "без такой пары в наборе правило `putIfAbsent` не проверено ничем"
    )
    val together = branches.filter { it.forkedFrom in shared }.map { demoLanes()[it.id] }
    assertEquals(
      together.size,
      together.distinct().size,
      "ветки от одной развилки живут одновременно и обязаны стоять на разных дорожках"
    )
  }

  @Test
  fun `branches merge on both sides of the trunk`() {
    val lanes = demoLanes()
    val merged = mockGraph().branches.filter { it.mergedAt != null }.mapNotNull { lanes[it.id] }

    assertTrue(
      merged.any { it > 0 } && merged.any { it < 0 },
      "чип «Закрыта» висит с той стороны, откуда ветка не возвращается, и сторона выводится из " +
        "знака дорожки — §12. С одним слиянием в наборе проверена ровно половина правила"
    )
  }

  @Test
  fun `the set keeps a branch node standing later than the front`() {
    val nodes = mockGraph().graphNodes
    val frontIndex = nodes.indexOfFirst { it.role == GraphNodeRole.Front }

    assertTrue(
      nodes.drop(frontIndex + 1).any { it.branchId != GraphBranch.Id("trunk") },
      "живая тема, идущая после того, как магистраль замолчала, — обычное состояние, " +
        "и хвост, привязанный к фронту, уходил бы на ней в отрицательную длину"
    )
  }

  @Test
  fun `every fork and merge of a branch is a node of the set`() {
    val ids = mockGraph().graphNodes.map { it.id }.toSet()
    val anchors = mockGraph().branches.flatMap { listOfNotNull(it.forkedFrom, it.mergedAt) }

    assertTrue(
      anchors.all { it in ids },
      "точки ветвления и слияния обязаны быть узлами списка: только собственный отрезок по X " +
        "не даёт вертикали ребра задеть чужую плашку"
    )
  }

  @Test
  fun `the demo set lays out on seven lanes with three of them reused`() {
    assertEquals(
      mapOf(
        GraphBranch.Id("trunk") to 0,
        GraphBranch.Id("terms") to 1,
        GraphBranch.Id("export") to -1,
        GraphBranch.Id("design") to 2,
        // Дорожка +1 освободилась слиянием terms и досталась budget.
        GraphBranch.Id("budget") to 1,
        GraphBranch.Id("photos") to -2,
        // logo уходит, пока photos ещё держит −2, поэтому встаёт за неё. Её вертикаль от магистрали
        // до третьей дорожки пересекает две чужие горизонтали — budget и design.
        GraphBranch.Id("logo") to 3,
        // Дорожка −2 освободилась слиянием photos.
        GraphBranch.Id("release") to -2,
        // Дорожка +3 освободилась **заброшенностью** logo, а не слиянием: второй способ отдать
        // дорожку, и в наборе он есть ровно здесь.
        GraphBranch.Id("pricing") to 3,
        GraphBranch.Id("stickers") to -3
      ),
      demoLanes(),
      "раскладка демо-набора: девять веток на шести дорожках плюс магистраль, " +
        "и три дорожки из шести переиспользованы"
    )
  }

  @Test
  fun `the demo set raises two hops on one vertical`() {
    val edges = demoEdges()

    assertTrue(
      edges.any { it.hops.size >= 2 },
      "мостики обязаны появиться парой на одной вертикали: ветка, уходящая на третью дорожку, " +
        "пересекает две чужие горизонтали, и одиночный мостик такого случая не проверяет"
    )
    val hops = edges.sumOf { it.hops.size }
    assertTrue(
      hops >= 6,
      "набор обязан ставить мостики и на уходах, и на возвратах, и на горизонтали магистрали. " +
        "Найдено: $hops"
    )
  }

  @Test
  fun `an abandoned branch has no tail`() {
    val abandoned = mockGraph().branches.single { it.status == GraphBranchStatus.Abandoned }
    val tails = demoEdges().filter { it.role == GraphEdgeRole.Tail }

    assertTrue(
      tails.isNotEmpty(),
      "хвост есть у всего, что не слито и не брошено: без него в наборе не проверено растворение"
    )
    assertTrue(
      tails.none { it.status == GraphBranchStatus.Abandoned },
      "линия брошенной темы обрывается сразу за последним узлом: тянуть её через всю историю " +
        "значило бы утверждать, что тема жива — §6.8. Ветка ${abandoned.id.value}"
    )
  }

  @Test
  fun `the set uses all six identity colours and wraps past the sixth`() {
    val colours = mockGraph().branches.map { it.colorIndex }

    assertEquals(
      (1..6).toList(),
      colours.distinct().sorted(),
      "шестая ветка сторожит остаток от деления, дававший ноль — цвет магистрали"
    )
    assertTrue(
      colours.size > 6 && colours.none { it == 0 },
      "седьмая и следующие обязаны брать оттенки по кругу, а не ноль: ноль оставлен магистрали, " +
        "и наивное `order mod 6` красило бы шестую ветку её цветом"
    )
  }

  @Test
  fun `every branch state of the brief is present`() {
    val branches = mockGraph().branches

    assertTrue(branches.any { it.mergedAt != null }, "слитая ветка")
    assertTrue(branches.any { it.status == GraphBranchStatus.Abandoned }, "заброшенная ветка")
    assertTrue(
      branches.any { it.status == GraphBranchStatus.Alive },
      "живая ветка: у неё хвост тянется до правого края содержимого"
    )
    assertTrue(
      branches.any { it.status == GraphBranchStatus.Waiting },
      "ветка с открытым merge request: её линия пунктирная"
    )
    assertTrue(
      branches.any { it.status == GraphBranchStatus.Ready },
      "готова к слиянию: пунктир тот же, отличает его бег вдоль линии"
    )
  }

  @Test
  fun `every node role and every time gap is present`() {
    val nodes = mockGraph().graphNodes

    assertEquals(
      GraphNodeRole.entries.toSet(),
      nodes.map { it.role }.toSet(),
      "род узла решает, что рисовать, и каждый из четырёх обязан быть в наборе"
    )
    assertEquals(
      TimeGap.entries.toSet(),
      nodes.map { it.gap }.toSet(),
      "все пять ступеней паузы обязаны встречаться: зазор считается по ним, и ступень, которой в " +
        "наборе нет, не проверена ничем"
    )
  }

  @Test
  fun `episode content covers what the card draws differently`() {
    val episodes = mockGraph().nodes.filter { it.node.role == GraphNodeRole.Episode }

    assertTrue(episodes.any { it.dim }, "эпизод внутри слитой ветки рисуется приглушённым")
    assertTrue(
      episodes.any { it.dim && it.unreadCount > 0 },
      "непрочитанное в закрытой теме остаётся: гаснет линия, а не плашка и не бейдж"
    )
    assertTrue(
      episodes.any { it.myShare == 0f } && episodes.any { it.myShare == 1f },
      "полоска реплик обязана быть в наборе и пустой, и заполненной: разговор в одни ворота — " +
        "единственное, что она отличает, пока текста в узле одна строка"
    )
    assertTrue(
      episodes.any { it.count == 1 } && episodes.any { it.count == 24 },
      "кластер бывает и из одного сообщения, и упёршимся в потолок §5 в двадцать четыре"
    )
    assertTrue(
      episodes.any { it.unreadCount > 99 },
      "трёхзначный счётчик обязан упереться в «99» и не растянуть плашку"
    )
    assertTrue(
      episodes.any { it.snippet.length > 100 },
      "длинный сниппет обязан упереться в фиксированную ширину узла эллипсисом"
    )
  }

  @Test
  fun `the empty graph lays out nothing`() {
    val graph = mockEmptyGraph()

    assertTrue(graph.graphNodes.isEmpty(), "пустая переписка — это переписка без истории, §13")
    assertTrue(
      graphEdgesOf(
        nodes = graph.graphNodes,
        branches = graph.branches,
        laneYs = emptyList(),
        positions = emptyList(),
        sizes = emptyList(),
        contentRight = 0f,
        hopClearance = 16f
      ).isEmpty(),
      "рисовать на пустом графе нечего, и выйти это должно из данных, а не из проверки на месте"
    )
  }

  @Test
  fun `the single episode graph stays on the trunk`() {
    val graph = mockSingleEpisodeGraph()

    assertEquals(1, graph.graphNodes.size, "одно сообщение и есть весь граф — §15 п. 1")
    assertEquals(
      listOf(0),
      graphLanesOf(graph.graphNodes, graph.branches),
      "единственный узел стоит на магистрали: обе оси камеры вырождаются в точку, и упор обязан " +
        "считаться упором, а не ошибкой"
    )
  }

  @Test
  fun `the linear graph keeps every node on the trunk`() {
    val graph = mockLinearGraph()

    assertTrue(graph.branches.isEmpty(), "переписка без веток — самый частый случай, §13")
    assertTrue(
      graphLanesOf(graph.graphNodes, graph.branches).all { it == 0 },
      "граф вырождается в прямую линию: дорожка у всех нулевая, и полотно обязано схлопнуться по " +
        "высоте, а не оставить место под пустые ряды"
    )
    assertTrue(
      graph.graphNodes.any { it.role == GraphNodeRole.Front },
      "фронт рисуется на любом уровне и в любом состоянии — он часть скелета смысла, §5"
    )
  }

  @Test
  fun `the crowded graph takes a lane past the ceiling`() {
    val graph = mockCrowdedGraph()
    val lanes = graphLanesOf(graph.graphNodes, graph.branches).filter { it != 0 }.distinct()

    assertEquals(
      graph.branches.size,
      lanes.size,
      "ни одна ветка набора не закрыта и не брошена, поэтому переиспользовать дорожку нечем"
    )
    assertTrue(
      lanes.size > 7,
      "потолок §4.2 в семь дорожек раскраска не ставит: восьмая ветка получает дорожку за потолком, " +
        "а не теряется — §18 п. 6 закрыт вертикальным скроллом полотна"
    )
  }

  /** Дорожка каждой ветки демо-набора, включая магистраль. */
  private fun demoLanes(): Map<GraphBranch.Id, Int> {
    val nodes = mockGraph().graphNodes
    val lanes = graphLanesOf(nodes, mockGraph().branches)
    return nodes.map { it.branchId }.zip(lanes).toMap()
  }

  /**
   * Рёбра демо-набора на синтетической раскладке.
   *
   * Плашки одной ширины и стоят с одинаковым шагом: проверяются пересечения и хвосты, а не
   * измерение, — а настоящие размеры пришли бы только из Compose.
   */
  private fun demoEdges(): List<GraphEdge> {
    val graph = mockGraph()
    val nodes = graph.graphNodes
    val lanes = graphLanesOf(nodes, graph.branches)
    return graphEdgesOf(
      nodes = nodes,
      branches = graph.branches,
      laneYs = lanes.map { it * 104f },
      positions = nodes.indices.map { index -> IntOffset(x = index * 340, y = 0) },
      sizes = List(nodes.size) { IntSize(width = 200, height = 72) },
      contentRight = nodes.size * 340f,
      hopClearance = 16f
    )
  }
}
