package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.TimeGap
import ru.sla.clarify.feature.chronology.ui.components.canvas.GraphGeometry
import ru.sla.clarify.feature.chronology.ui.components.canvas.fitScaleOf
import ru.sla.clarify.feature.chronology.ui.components.canvas.graphEdgesOf
import ru.sla.clarify.feature.chronology.ui.components.canvas.graphLevelBandOf
import ru.sla.clarify.feature.chronology.ui.components.canvas.graphPlacementOf
import ru.sla.clarify.feature.chronology.ui.components.canvas.mockBranchColors
import ru.sla.clarify.feature.chronology.ui.components.canvas.mockLanes
import ru.sla.clarify.feature.chronology.ui.components.canvas.topLaneOf
import ru.sla.clarify.feature.chronology.ui.entity.GraphCanvasMargins
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdge
import ru.sla.clarify.feature.chronology.ui.entity.GraphEdgeRole
import ru.sla.clarify.feature.chronology.ui.entity.GraphLevel
import ru.sla.clarify.feature.chronology.ui.entity.Node
import ru.sla.clarify.feature.chronology.ui.mapper.toLaneStep
import ru.sla.clarify.feature.chronology.ui.mapper.toStepWidth

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
      demoLanes()[Branch.Id("terms")] != demoLanes()[Branch.Id("design")],
      "ветка design уходит с магистрали до слияния terms, и делить дорожку им нельзя: " +
        "горизонталь возврата terms прошла бы сквозь плашки design"
    )
  }

  @Test
  fun `a merged branch hands its lane over to a later one`() {
    val lanes = demoLanes()

    assertEquals(
      lanes[Branch.Id("terms")],
      lanes[Branch.Id("budget")],
      "terms слита раньше, чем budget ответвилась, и дорожка обязана переиспользоваться — §4.2"
    )
    assertEquals(
      lanes[Branch.Id("photos")],
      lanes[Branch.Id("release")],
      "второе слияние обязано отдавать дорожку так же, как первое: одного примера в наборе мало, " +
        "потому что он не отличает правило от совпадения"
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
  fun `branches merge on both sides of the baseline`() {
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
    val graph = mockGraph().layout
    val frontIndex = graph.nodes.indexOfFirst { it is Node.Front }

    assertTrue(
      graph.nodes.drop(frontIndex + 1).any { graph.branchOf(it.id).id != Branch.Id("baseline") },
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
  fun `the demo set lays out on seven lanes with two of them reused`() {
    assertEquals(
      mapOf(
        Branch.Id("baseline") to 0,
        Branch.Id("terms") to 1,
        Branch.Id("export") to -1,
        Branch.Id("design") to 2,
        // Дорожка +1 освободилась слиянием terms и досталась budget.
        Branch.Id("budget") to 1,
        Branch.Id("photos") to -2,
        // Дорожка −2 освободилась слиянием photos.
        Branch.Id("release") to -2,
        // Свободных дорожек ближе к магистрали не осталось: pricing уходит от той же развилки, что
        // release, и её вертикаль пересекает две чужие горизонтали — budget и design.
        Branch.Id("pricing") to 3,
        Branch.Id("stickers") to -3
      ),
      demoLanes(),
      "раскладка демо-набора: восемь веток на шести дорожках плюс магистраль, " +
        "и две дорожки из шести переиспользованы"
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
  fun `every unmerged branch of the set ends in a tail`() {
    val unmerged = mockGraph().branches.count { it.mergedAt == null }
    val tails = demoEdges().filter { it.role == GraphEdgeRole.Tail }

    assertEquals(
      unmerged,
      tails.size,
      "хвост есть у всего, что не слито, — §6.8: тема не закрыта, сколько бы она ни молчала. " +
        "Исключением была брошенная тема, и вместе с ней исключение отменено"
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
    assertTrue(
      branches.any { it.status == Branch.Status.Alive },
      "живая ветка: у неё хвост тянется до правого края содержимого"
    )
    assertTrue(
      branches.any { it.status == Branch.Status.Waiting },
      "ветка с открытым merge request: её линия пунктирная"
    )
    assertTrue(
      branches.any { it.status == Branch.Status.Ready },
      "готова к слиянию: пунктир тот же, отличает его бег вдоль линии"
    )
  }

  @Test
  fun `every node role and every time gap is present`() {
    val nodes = mockGraph().graphNodes

    assertEquals(
      setOf(Node.Episode::class, Node.Fork::class, Node.Merge::class, Node.Front::class),
      nodes.map { it::class }.toSet(),
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
    val episodes = mockGraph().graphNodes.filterIsInstance<Node.Episode>()

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
        graph = graph.layout,
        branchColors = graph.layout.mockBranchColors(),
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
  fun `the single episode graph stays on the baseline`() {
    val graph = mockSingleEpisodeGraph()

    assertEquals(1, graph.graphNodes.size, "одно сообщение и есть весь граф — §15 п. 1")
    assertEquals(
      listOf(0),
      graph.layout.mockLanes().lanes,
      "единственный узел стоит на магистрали: обе оси камеры вырождаются в точку, и упор обязан " +
        "считаться упором, а не ошибкой"
    )
  }

  @Test
  fun `the linear graph keeps every node on the baseline`() {
    val graph = mockLinearGraph()

    assertTrue(graph.branches.isEmpty(), "переписка без веток — самый частый случай, §13")
    assertTrue(
      graph.layout.mockLanes().lanes.all { it == 0 },
      "граф вырождается в прямую линию: дорожка у всех нулевая, и полотно обязано схлопнуться по " +
        "высоте, а не оставить место под пустые ряды"
    )
    assertTrue(
      graph.graphNodes.any { it is Node.Front },
      "фронт рисуется на любом уровне и в любом состоянии — он часть скелета смысла, §5"
    )
  }

  @Test
  fun `the crowded graph takes a lane past the ceiling`() {
    val graph = mockCrowdedGraph()
    val lanes = graph.layout.mockLanes().lanes.filter { it != 0 }.distinct()

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

  /**
   * Тап по узлу без содержимого карточки не заводится вовсе, поэтому эпизод без превью — это узел,
   * который молча перестал нажиматься. По коду этого не видно: и поле, и его чтение на месте.
   */
  @Test
  fun `every episode of the set has a card to open`() {
    val graph = mockGraph()
    val episodes = graph.graphNodes.filterIsInstance<Node.Episode>()

    assertEquals(
      episodes.map { it.id }.toSet(),
      graph.previewById.keys,
      "эпизод без превью перестаёт нажиматься, и заметить это можно только тапнув по нему"
    )
  }

  /**
   * Подпись узла для скринридера называет ветку, а имени у [Branch] нет: оно живёт отдельной
   * картой, и разъехаться с набором ей ничто не мешает.
   */
  @Test
  fun `every branch of the set has a name`() {
    val graph = mockGraph()

    assertEquals(
      graph.branches.map { it.id }.toSet(),
      graph.branchNames.keys,
      "ветка без имени озвучится скринридеру как безымянная, и заметить это можно только вслух"
    )
  }

  /**
   * Вся арифметика обзора посчитана на этом наборе, и держится она на его составе: двадцать четыре
   * эпизода, восемь точек и фронт при зазорах, делённых на четыре, дают 1352 dp и «вписать всё» на
   * 0.305×. Первая же добавленная ветка это число сдвинет, а увидеть сдвиг можно только на
   * устройстве — на глаз обзор выглядит одинаково и при 0.3×, и при 0.15×, пока не вглядишься в глиф.
   */
  @Test
  fun `the demo graph still fits the overview at its own floor`() {
    val graph = mockGraph()
    val nodes = graph.graphNodes
    val lanes = graph.layout.mockLanes().lanes
    val geometry = GraphGeometry(topLaneOf(lanes), GraphLevel.Overview.toLaneStep())
    val placement = graphPlacementOf(
      lanes = lanes,
      gaps = nodes.map { it.gap.toStepWidth(GraphLevel.Overview).value },
      laneYs = lanes.map { geometry.laneYOf(it).value },
      sizes = nodes.map { it.toOverviewSize() },
      margins = GraphCanvasMargins(left = 64f, top = 64f, right = 64f, bottom = 64f)
    )

    val fit = fitScaleOf(placement.bounds, IntSize(width = 412, height = 892))

    assertTrue(
      fit > graphLevelBandOf(GraphLevel.Overview, fit).min - 1e-4f,
      "нижний край обзора и есть вписанный граф: набор, переросший его, вписывается уже не целиком"
    )
    assertTrue(
      fit > 0.2f,
      "ниже 0.2× глиф 14 dp вырождается в три пикселя, и обзор перестаёт быть картой: " +
        "набор обязан оставаться в этих пределах, иначе фичу смотрят на том, чего она не умеет"
    )
  }

  /** Размер узла на обзоре: плашка вырождается в глиф, точки на линии остаются собой. */
  private fun Node.toOverviewSize(): IntSize {
    return when (this) {
      is Node.Episode -> IntSize(width = 14, height = 14)
      is Node.Fork, is Node.Merge -> IntSize(width = 24, height = 24)
      is Node.Front -> IntSize(width = 12, height = 12)
    }
  }

  /** Дорожка каждой ветки демо-набора, включая магистраль. */
  private fun demoLanes(): Map<Branch.Id, Int> {
    val graph = mockGraph().layout
    return graph.branchIds.zip(graph.mockLanes().lanes).toMap()
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
    val lanes = graph.layout.mockLanes().lanes
    return graphEdgesOf(
      graph = graph.layout,
      branchColors = graph.layout.mockBranchColors(),
      laneYs = lanes.map { it * 104f },
      positions = nodes.indices.map { index -> IntOffset(x = index * 340, y = 0) },
      sizes = List(nodes.size) { IntSize(width = 200, height = 72) },
      contentRight = nodes.size * 340f,
      hopClearance = 16f
    )
  }
}
