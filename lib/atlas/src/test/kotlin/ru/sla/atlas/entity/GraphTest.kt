package ru.sla.atlas.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Сторожит обратную сторону состава ветки: «что случилось в этом узле».
 *
 * Ветка помнит свою развилку и своё слияние, а спрашивают обычно наоборот — глядя на узел. Пока
 * ответа у графа не было, каждый спрашивающий строил индекс сам, и делали это уже двое: раскладка
 * ради акцента узла и вызывающий ради того, что он рисует рядом. Второй такой индекс расходится
 * молча — он выглядит
 * тем же перебором и отличается только тем, кто выигрывает при совпадении.
 */
class GraphTest {

  @Test
  fun `a node carrying a fork names the branch that left it`() {
    val graph = graphOf(mockBranch(id = "a", nodes = mockNodeNames(3..5), forkedFrom = "n2"))

    assertEquals(
      Branch.Id("a"),
      graph.branchForkedAt(BasicNode.Id("n2")),
      "узел ветвления стоит на магистрали и называет ушедшую от него ветку"
    )
  }

  @Test
  fun `a node carrying a merge names the branch that came back`() {
    val graph = graphOf(mockBranch(id = "a", nodes = mockNodeNames(3..5), forkedFrom = "n2", mergedAt = "n8"))

    assertEquals(
      Branch.Id("a"),
      graph.branchMergedAt(BasicNode.Id("n8")),
      "узел слияния называет вернувшуюся в магистраль ветку"
    )
  }

  @Test
  fun `a plain node carries neither`() {
    val graph = graphOf(mockBranch(id = "a", nodes = mockNodeNames(3..5), forkedFrom = "n2", mergedAt = "n8"))

    assertNull(graph.branchForkedAt(BasicNode.Id("n6")), "в обычном узле ничего не случилось")
    assertNull(graph.branchMergedAt(BasicNode.Id("n6")), "в обычном узле ничего не случилось")
  }

  @Test
  fun `two branches leaving one node answer with the earlier one`() {
    // Два ответа на один вопрос дать нельзя, а веток от одного коммита уходит сколько угодно.
    val first = mockBranch(id = "a", nodes = mockNodeNames(3..4), forkedFrom = "n2")
    val second = mockBranch(id = "b", nodes = mockNodeNames(5..6), forkedFrom = "n2")
    val graph = graphOf(first, second)

    assertEquals(
      Branch.Id("a"),
      graph.branchForkedAt(BasicNode.Id("n2")),
      "отвечает первая по порядку ветвления; вторая не теряется — её развилку спрашивают у неё самой"
    )
  }

  @Test
  fun `the baseline carries no fork of its own`() {
    val graph = graphOf(mockBranch(id = "a", nodes = mockNodeNames(3..5), forkedFrom = "n2"))

    assertNull(
      graph.branchForkedAt(BasicNode.Id("n0")),
      "у магистрали ни развилки, ни слияния нет по определению — уходить ей неоткуда"
    )
  }

  /**
   * Граф на десять узлов: магистраль забирает всё, чего не перечислили ветки.
   *
   * @param branches ветки, кроме магистрали
   * @return граф, прошедший проверку состава
   */
  private fun graphOf(vararg branches: Branch): Graph<MockNode> {
    val nodes = mockNodes(count = 10)
    val taken = branches.flatMap { it.nodeIds }.toSet()
    return Graph(
      nodes = nodes,
      baseline = mockBranch(id = "baseline", colorIndex = 0).copy(
        nodeIds = nodes.map { it.id }.filterNot { taken.contains(it) }
      ),
      branches = branches.toList()
    )
  }
}
