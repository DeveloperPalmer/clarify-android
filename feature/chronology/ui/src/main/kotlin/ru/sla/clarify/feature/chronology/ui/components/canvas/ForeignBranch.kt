package ru.sla.clarify.feature.chronology.ui.components.canvas

import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode

/**
 * Чужая ветка, за которую говорит узел беседы: та, что от него ушла или в него вернулась.
 *
 * Живёт в фиче, а не в раскладке, потому что это знание о родах узлов. Что в узле случилось
 * ветвление, знает граф; что узел рисует собой ушедшую ветку — §6.4 и §6.6 брифа, и знать это
 * раскладке неоткуда.
 *
 * Ветка ищется по узлу, а не по порядку: от одного коммита может уйти несколько веток, и тогда
 * акцент точке задаёт первая из них, а остальные получают свои собственные точки ветвления — по
 * одной на ветку.
 *
 * @param node узел графа
 * @return чужая ветка, чьи цвет и направление узел показывает; `null` — чужой нет, и свою
 *   подставит раскладка
 */
internal fun Graph<GraphNode>.foreignBranchOf(node: GraphNode): Branch.Id? {
  return when (node) {
    is GraphNode.Fork -> branchForkedAt(node.id)
    is GraphNode.Merge -> branchMergedAt(node.id)
    is GraphNode.Episode,
    is GraphNode.Front -> null
  }
}
