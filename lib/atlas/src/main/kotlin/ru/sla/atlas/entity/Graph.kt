package ru.sla.atlas.entity

import androidx.compose.runtime.Immutable

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
 * обещает [BasicNode]; чем узел является и что внутри него нарисовано, знает та сторона, которая
 * его рисует. Держи граф сам [BasicNode] — и вызывающий получал бы свои узлы обратно приведением
 * типа на каждом обращении, то есть там, где ошибка видна позже всего.
 *
 * @param N узел вызывающего
 * @param nodes узлы в хронологическом порядке; порядок и есть ось X
 * @param baseline магистраль: ветка, от которой уходят остальные
 * @param branches ветки, кроме магистрали, в порядке ветвления — в этом же порядке идёт жадная раскладка дорожек
 */
@Immutable
data class Graph<out N : BasicNode>(
  val nodes: List<N>,
  val baseline: Branch,
  val branches: List<Branch>
) {

  /** Порядковый номер каждого узла: порядок — свойство графа, и считается он один раз. */
  val nodeIndexesById: Map<BasicNode.Id, Int> = nodes
    .withIndex()
    .associate { (index, node) -> node.id to index }

  /** Ветка каждого узла, в порядке [nodes]: обратная сторона [Branch.nodeIds]. */
  val branchIds: List<Branch.Id>

  private val branchIdByNode: Map<BasicNode.Id, Branch.Id>

  private val branchesById: Map<Branch.Id, Branch> = (listOf(baseline) + branches).associateBy { it.id }

  init {
    val owners = HashMap<BasicNode.Id, Branch.Id>(nodes.size)
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
  fun branchOf(nodeId: BasicNode.Id): Branch {
    return branchesById.getValue(branchIdByNode.getValue(nodeId))
  }

  companion object {

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
        colorIndex = 0,
        forkedFrom = null,
        mergedAt = null,
        status = Branch.Status.Alive
      ),
      branches = emptyList()
    )
  }
}
