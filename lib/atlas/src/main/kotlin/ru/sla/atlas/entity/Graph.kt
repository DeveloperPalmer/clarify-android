package ru.sla.atlas.entity

import androidx.compose.runtime.Immutable
import ru.sla.atlas.mapper.toBranch
import java.time.Duration

/**
 * Граф целиком: порядок узлов и состав веток.
 *
 * Две разные истины, и потому они лежат в разных местах. Порядок принадлежит списку [nodes] — он и
 * есть ось: расстояние копится слева направо, и пауза узла отмеряется от предыдущего узла **этого**
 * списка. Состав принадлежит ветке — [Branch.nodeIds]. Сойтись они обязаны, и сводит их этот класс.
 *
 * **Инвариант проверяется в конструкторе, потому что больше его проверить негде.** Пока ветку знал
 * сам узел, принадлежность была тотальной и однозначной по построению; состав, отданный ветке, эту
 * гарантию снимает — узел может остаться ничьим, попасть в две ветки или быть перечисленным веткой,
 * не существуя в графе. Ни одно из трёх не видно на экране: узел просто уезжает на нулевую дорожку.
 *
 * Магистраль названа отдельным полем, а не первой в [branches]. Она и правда особенная: раскладка
 * держит её на нулевой дорожке и в раскраску не пускает, — а «первый элемент списка» это соглашение,
 * которое ломается молча при первой же сортировке.
 *
 * **Тип узла задаёт вызывающий.** Графу от узла нужны только идентификатор и пауза — ровно то, что
 * обещает [Node]; чем узел является и что внутри него нарисовано, знает та сторона, которая его
 * рисует. Держи граф список самих [Node] вместо [N] — и вызывающий получал бы свои узлы обратно
 * приведением типа на каждом обращении, то есть там, где ошибка видна позже всего.
 *
 * @param N узел вызывающего
 * @param nodes узлы в хронологическом порядке; порядок и есть ось X
 * @param baseline магистраль: ветка, от которой уходят остальные
 * @param branches ветки, кроме магистрали, в порядке ветвления — в этом же порядке идёт жадная раскладка дорожек
 */
@Immutable
data class Graph<out N : Node>(
  val nodes: List<N>,
  val baseline: Branch,
  val branches: List<Branch>
) {

  /** Порядковый номер каждого узла: порядок — свойство графа, и считается он один раз. */
  val nodeIndexesById: Map<Node.Id, Int> = nodes
    .withIndex()
    .associate { (index, node) -> node.id to index }

  /** Ветка каждого узла, в порядке [nodes]: обратная сторона [Branch.nodeIds]. */
  val branchIds: List<Branch.Id>

  private val branchIdByNode: Map<Node.Id, Branch.Id>

  private val branchesById: Map<Branch.Id, Branch> = (listOf(baseline) + branches).associateBy { it.id }

  // Обратная сторона [Branch.forkedFrom] и [Branch.mergedAt]: ветка помнит свой узел, а спрашивают
  // обычно наоборот — «что случилось в этом узле». Считаются один раз, как [nodeIndexesById]: иначе
  // каждый спрашивающий строил бы их заново, а спрашивают на каждый узел графа.
  private val branchByFork: Map<Node.Id, Branch.Id> = branchIndexOf(branches) { it.forkedFrom }

  private val branchByMerge: Map<Node.Id, Branch.Id> = branchIndexOf(branches) { it.mergedAt }

  init {
    val owners = HashMap<Node.Id, Branch.Id>(nodes.size)
    branchesById.values.forEach { branch ->
      branch.nodeIds.forEach { nodeId ->
        require(nodeIndexesById.containsKey(nodeId)) {
          "Ветка ${branch.id.value} перечисляет узел ${nodeId.value}, которого нет в графе"
        }
        val known = owners.put(nodeId, branch.id)
        require(known == null) {
          // `orEmpty` недостижим: сообщение считается только тогда, когда прежний владелец есть.
          "Узел ${nodeId.value} принадлежит и ветке ${known?.value.orEmpty()}, " +
            "и ветке ${branch.id.value}"
        }
      }
    }
    require(owners.size == nodes.size) {
      val orphans = nodes.filterNot { owners.containsKey(it.id) }.map { it.id.value }
      "Узлы $orphans не принадлежат ни одной ветке"
    }
    branchIdByNode = owners
    branchIds = nodes.map { owners.getValue(it.id) }
  }

  /**
   * Ветка, которой принадлежит узел.
   *
   * @param nodeId узел графа
   * @return его ветка; для узла не из этого графа — ошибка, потому что чужой узел здесь означает
   *   рассинхронизацию наборов, а не отсутствующее значение
   */
  fun branchOf(nodeId: Node.Id): Branch {
    return branchesById.getValue(branchIdByNode.getValue(nodeId))
  }

  /**
   * Ветка, ушедшая от этого узла.
   *
   * От одного узла ветка уходит не обязательно одна, а ответ здесь один: отвечает первая по порядку
   * ветвления. Остальные не теряются — у каждой ветки развилка своя, и спрашивают о ней по её
   * собственному [Branch.forkedFrom].
   *
   * Отдаётся тождество, а не ветка, в отличие от [branchOf]: по этому ответу ищут дорожку и цвет,
   * то есть спрашивают именно «чья», а не «какая».
   *
   * @param nodeId узел графа
   * @return ветка, чья развилка стоит на этом узле; `null` — обычный узел
   */
  fun branchForkedAt(nodeId: Node.Id): Branch.Id? {
    return branchByFork[nodeId]
  }

  /**
   * Ветка, вернувшаяся в родителя в этом узле.
   *
   * Парная к [branchForkedAt] и с тем же правилом на совпадение.
   *
   * @param nodeId узел графа
   * @return ветка, чьё слияние стоит на этом узле; `null` — обычный узел
   */
  fun branchMergedAt(nodeId: Node.Id): Branch.Id? {
    return branchByMerge[nodeId]
  }

  companion object {

    /**
     * Граф из черновиков: расставляет узлы по времени, проставляет паузы и раздаёт веткам их состав.
     *
     * **Собирает граф тот, кто проверяет его состав.** Конструктор требует, чтобы каждый узел
     * принадлежал ровно одной ветке, и ловит нарушение на месте — а собирали граф до сих пор этажом
     * выше, где этой проверки нет. Три величины, которые там сходились руками, сходятся здесь:
     * порядок узлов, паузы между ними и обратная сторона порядка — состав каждой ветки.
     *
     * **Порядок задаёт время, а не ветка.** Ось X накапливается по порядку списка, поэтому набор,
     * сгруппированный по веткам, поставил бы позднюю ветку левее ранней. Узлы всех веток сводятся в
     * один список и сортируются один раз, и только после этого у каждого появляется пауза: она
     * считается от соседа **по времени**, а кто сосед, до сортировки неизвестно.
     *
     * **Пауза меряется по соседу в списке, а не по соседу той же ветки.** Зазор раздвигает узлы по
     * общей оси, и пауза внутри ветки, посчитанная в обход чужих узлов, поставила бы её узлы поверх
     * них.
     *
     * Перед первым узлом паузы нет: отступ от края полотна дают поля, а не выдуманный зазор.
     *
     * @param N узел вызывающего
     * @param drafts узлы в любом порядке: в нужный их поставит сортировка
     * @param baseline магистраль: [BranchDraft], потому что состав веток — это и есть то, что сборка
     *   считает, и подать его вместе с веткой значило бы подать выдумку
     * @param branches остальные ветки, тоже черновиками
     * @param scale шкала узлов вызывающего: чем меряется пауза и как она попадает в узел
     * @return граф, прошедший проверку состава
     */
    fun <N : Node> of(
      drafts: List<NodeDraft<N>>,
      baseline: BranchDraft,
      branches: List<BranchDraft>,
      scale: NodeScale<N>
    ): Graph<N> {
      val ordered = drafts.sortedWith(compareBy({ it.at }, { it.order }))
      val nodes = ordered.mapIndexed { index, draft ->
        val previous = ordered.getOrNull(index - 1)
        val gap = scale.gapOf(Duration.between(previous?.at ?: draft.at, draft.at))
        scale.withGap(draft.node, gap)
      }
      val nodeIdsByBranch = ordered.groupBy({ it.branchId }, { it.node.id })
      return Graph(
        nodes = nodes,
        baseline = baseline.toBranch(nodeIdsByBranch[baseline.id].orEmpty()),
        branches = branches.map { branch -> branch.toBranch(nodeIdsByBranch[branch.id].orEmpty()) }
      )
    }

    /**
     * Граф, которого ещё нет: ни узлов, ни веток.
     *
     * Нужен затем, чтобы полотно жило до первой загрузки, не заводя ветку-заглушку в каждом
     * вызывающем. Тождество магистрали здесь пустое — искать по нему нечего и не в чем.
     */
    val Empty = Graph<Nothing>(
      nodes = emptyList(),
      baseline = Branch(
        id = Branch.Id(""),
        nodeIds = emptyList(),
        forkedFrom = null,
        mergedAt = null,
        status = Branch.Status.Alive
      ),
      branches = emptyList()
    )
  }
}

/**
 * Индекс «узел → ветка» по одному из узлов-ориентиров ветки.
 *
 * Первая ветка выигрывает: от одного узла может уйти несколько веток, и отдать точке ветвления
 * нужно одну — ту, что ушла раньше по порядку ветвления.
 *
 * @param branches ветки графа, кроме магистрали: у неё ни развилки, ни слияния нет по определению
 * @param nodeOf узел-ориентир ветки — развилка или слияние
 * @return ветка по её узлу-ориентиру
 */
private fun branchIndexOf(
  branches: List<Branch>,
  nodeOf: (Branch) -> Node.Id?
): Map<Node.Id, Branch.Id> {
  val index = HashMap<Node.Id, Branch.Id>()
  branches.forEach { branch ->
    nodeOf(branch)?.let { index.putIfAbsent(it, branch.id) }
  }
  return index
}
