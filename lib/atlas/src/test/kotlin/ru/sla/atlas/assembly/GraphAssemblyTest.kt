package ru.sla.atlas.assembly

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sla.atlas.entity.BasicNode
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.MockNode
import ru.sla.atlas.entity.NodeDraft
import ru.sla.atlas.entity.TimeGap
import ru.sla.atlas.entity.mockBranch
import ru.sla.atlas.entity.mockNode
import java.time.Duration
import java.time.LocalDateTime

/**
 * Сторожит три величины, которые сходились руками у вызывающего и потому расходились.
 *
 * Порядок узлов задаёт время, а не ветка: набор, сгруппированный по веткам, поставил бы позднюю
 * ветку левее ранней. Пауза меряется по соседу **в общем списке**, а не по соседу той же ветки —
 * иначе узлы ветки легли бы поверх чужих. Состав ветки — обратная сторона того же порядка, и
 * заполнять его отдельным проходом значит завести вторую истину о принадлежности узла.
 */
class GraphAssemblyTest {

  @Test
  fun `nodes end up in the order of their time, whatever branch they belong to`() {
    val graph = assembled(
      draft("late", branch = "a", minutes = 30),
      draft("early", branch = "b", minutes = 0),
      draft("middle", branch = "a", minutes = 15)
    )

    assertEquals(
      listOf("early", "middle", "late"),
      graph.nodes.map { it.id.value },
      "ось накапливается по порядку списка, и сгруппированный по веткам набор поставил бы ветку от " +
        "тридцатой минуты левее ветки от нулевой"
    )
  }

  @Test
  fun `nodes sharing a moment are separated by their order`() {
    val graph = assembled(
      draft("second", branch = "a", minutes = 10, order = 1),
      draft("first", branch = "a", minutes = 10, order = 0)
    )

    assertEquals(
      listOf("first", "second"),
      graph.nodes.map { it.id.value },
      "совпадение моментов — обычное дело, и чем оно разрешается, знает вызывающий"
    )
  }

  @Test
  fun `a pause is measured against the neighbour in the list, not in the branch`() {
    val graph = assembled(
      draft("a1", branch = "a", minutes = 0),
      draft("b1", branch = "b", minutes = 50),
      draft("a2", branch = "a", minutes = 60)
    )

    assertEquals(
      listOf(TimeGap.Minutes, TimeGap.Long, TimeGap.Minutes),
      graph.nodes.map { it.gap },
      "второй узел ветки `a` отстоит от соседа по списку на десять минут, а не на час от своего " +
        "предшественника по ветке: иначе его плашка легла бы поверх чужой"
    )
  }

  @Test
  fun `the first node has no pause before it`() {
    val graph = assembled(draft("only", branch = "a", minutes = 42))

    assertEquals(
      TimeGap.Minutes,
      graph.nodes.single().gap,
      "отступ от края полотна дают поля, а не выдуманный зазор перед первым узлом"
    )
  }

  @Test
  fun `every branch gets the nodes that named it`() {
    val graph = assembled(
      draft("t1", branch = "trunk", minutes = 0),
      draft("a1", branch = "a", minutes = 10),
      draft("t2", branch = "trunk", minutes = 20)
    )

    assertEquals(
      listOf(BasicNode.Id("t1"), BasicNode.Id("t2")),
      graph.baseline.nodeIds,
      "состав ветки — обратная сторона порядка узлов, и собирается он тем же проходом"
    )
    assertEquals(Branch.Id("a"), graph.branchOf(BasicNode.Id("a1")).id)
  }

  @Test
  fun `a branch without nodes keeps an empty composition`() {
    val graph = assembled(draft("t1", branch = "trunk", minutes = 0))

    assertEquals(
      emptyList<BasicNode.Id>(),
      graph.branches.single { it.id == Branch.Id("a") }.nodeIds,
      "ветка, чьи узлы ещё не догружены, остаётся в графе пустой, а не роняет сборку"
    )
  }

  /**
   * Черновик узла в заданной минуте от начала отсчёта.
   *
   * @param id идентификатор узла
   * @param branch ветка, которой узел принадлежит
   * @param minutes минута от начала отсчёта
   * @param order разрешение спора внутри одной минуты
   * @return черновик, готовый попасть в сборку
   */
  private fun draft(id: String, branch: String, minutes: Long, order: Int = 0): NodeDraft<MockNode> {
    return NodeDraft(
      node = mockNode(id),
      branchId = Branch.Id(branch),
      at = START.plusMinutes(minutes),
      order = order
    )
  }

  /**
   * Граф из черновиков: магистраль `trunk`, рядом ветки `a` и `b`.
   *
   * @param drafts узлы в произвольном порядке
   * @return собранный граф
   */
  private fun assembled(vararg drafts: NodeDraft<MockNode>): ru.sla.atlas.entity.Graph<MockNode> {
    return graphOf(
      drafts = drafts.toList(),
      baseline = mockBranch(id = "trunk", colorIndex = 0),
      branches = listOf(mockBranch(id = "a"), mockBranch(id = "b")),
      gapOf = { duration -> if (duration > Duration.ofMinutes(20)) TimeGap.Long else TimeGap.Minutes },
      withGap = { node, gap -> node.copy(gap = gap) }
    )
  }
}

// Начало отсчёта: конкретная дата здесь ни на что не влияет — важны только промежутки между узлами.
private val START: LocalDateTime = LocalDateTime.of(2026, 3, 6, 10, 0)
