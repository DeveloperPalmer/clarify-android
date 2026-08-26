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
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeRole

/**
 * Сторожит **сценарии**, ради которых демо-набор и собран, а не его содержимое.
 *
 * Набор — единственное, на чём фича проверяется глазами, и каждая его ветка отвечает за состояние,
 * которое рисуется по-своему. Убрав одну, легко не заметить, что вместе с ней с экрана пропала
 * целая ветвь поведения: прежний набор два десятка итераций притворялся графом, не имея ни одной
 * настоящей ветки, и вскрылось это только когда мини-карта показала его в другой проекции.
 *
 * Поэтому тест проверяет не «сколько узлов», а «остались ли в наборе те положения, из-за которых
 * раскладка однажды сломалась».
 */
class MocksTest {

  @Test
  fun `a branch forked between a neighbour's last node and its merge keeps its own lane`() {
    val nodes = mockNodes().map { it.node }
    val lanes = graphLanesOf(nodes, mockBranches())
    val laneOf = nodes.map { it.branchId }.zip(lanes).toMap()

    assertTrue(
      laneOf[GraphBranch.Id("terms")] != laneOf[GraphBranch.Id("design")],
      "ветка design уходит с магистрали до слияния terms, и делить дорожку им нельзя: " +
        "горизонталь возврата terms прошла бы сквозь плашки design"
    )
  }

  @Test
  fun `a merged branch hands its lane over to a later one`() {
    val nodes = mockNodes().map { it.node }
    val lanes = graphLanesOf(nodes, mockBranches())
    val laneOf = nodes.map { it.branchId }.zip(lanes).toMap()

    assertEquals(
      laneOf[GraphBranch.Id("terms")],
      laneOf[GraphBranch.Id("budget")],
      "terms слита раньше, чем budget ответвилась, и дорожка обязана переиспользоваться — §4.2"
    )
  }

  @Test
  fun `the set keeps a branch node standing later than the front`() {
    val nodes = mockNodes().map { it.node }
    val frontIndex = nodes.indexOfFirst { it.role == GraphNodeRole.Front }

    assertTrue(
      nodes.drop(frontIndex + 1).any { it.branchId != GraphBranch.Id("trunk") },
      "живая тема, идущая после того, как магистраль замолчала, — обычное состояние, " +
        "и хвост, привязанный к фронту, уходил бы на ней в отрицательную длину"
    )
  }

  @Test
  fun `every fork and merge of a branch is a node of the set`() {
    val ids = mockNodes().map { it.node.id }.toSet()
    val anchors = mockBranches().flatMap { listOfNotNull(it.forkedFrom, it.mergedAt) }

    assertTrue(
      anchors.all { it in ids },
      "точки ветвления и слияния обязаны быть узлами списка: только собственный отрезок по X " +
        "не даёт вертикали ребра задеть чужую плашку"
    )
  }

  @Test
  fun `the demo set lays out on six lanes with one of them reused`() {
    val nodes = mockNodes().map { it.node }
    val lanes = graphLanesOf(nodes, mockBranches())
    val laneOf = nodes.map { it.branchId }.zip(lanes).toMap()

    assertEquals(
      mapOf(
        GraphBranch.Id("trunk") to 0,
        GraphBranch.Id("terms") to 1,
        GraphBranch.Id("export") to -1,
        GraphBranch.Id("design") to 2,
        // Дорожка +1 освободилась слиянием terms и досталась budget.
        GraphBranch.Id("budget") to 1,
        GraphBranch.Id("logo") to -2,
        // release уходит, пока брошенная logo ещё держит −2, поэтому встаёт за неё. Её вертикаль
        // от магистрали до третьей дорожки пересекает две чужие горизонтали — budget и design, —
        // и это единственное место набора, где на одной вертикали нужны два мостика.
        GraphBranch.Id("release") to 3
      ),
      laneOf,
      "раскладка демо-набора: шесть веток на шести дорожках, одна из них переиспользована"
    )
  }

  @Test
  fun `the demo set raises two hops on the vertical of the sixth branch`() {
    val nodes = mockNodes().map { it.node }
    val branches = mockBranches()
    val lanes = graphLanesOf(nodes, branches)
    // Плашки одной ширины: тест про пересечения, а не про измерение.
    val sizes = List(nodes.size) { IntSize(width = 200, height = 72) }
    val positions = nodes.indices.map { index -> IntOffset(x = index * 340, y = 0) }
    val edges = graphEdgesOf(
      nodes = nodes,
      branches = branches,
      laneYs = lanes.map { it * 104f },
      positions = positions,
      sizes = sizes,
      contentRight = nodes.size * 340f,
      hopClearance = 16f
    )

    val hops = edges.sumOf { it.hops.size }
    assertTrue(
      hops >= 3,
      "мостики обязаны появиться: release уходит на третью дорожку через две чужие горизонтали, " +
        "а design и logo — через одну каждая. Найдено: $hops"
    )
  }

  @Test
  fun `the set uses all six identity colours`() {
    assertEquals(
      (1..6).toList(),
      mockBranches().map { it.colorIndex }.sorted(),
      "шестая ветка сторожит остаток от деления, дававший ноль — цвет магистрали"
    )
  }

  @Test
  fun `every branch state of the brief is present`() {
    val branches = mockBranches()

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
      "готова к слиянию: пунктир тот же, отличает иконка терминатора"
    )
  }
}
