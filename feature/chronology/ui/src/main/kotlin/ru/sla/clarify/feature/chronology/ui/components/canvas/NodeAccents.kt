package ru.sla.clarify.feature.chronology.ui.components.canvas

import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode

/**
 * Ветка, за которую говорит узел беседы: своя у плашки, чужая у точки ветвления и точки слияния.
 *
 * Живёт в фиче, а не в раскладке, потому что это знание о родах узлов. Что в узле случилось
 * ветвление, знает граф; что узел рисует собой ушедшую ветку — §6.4 и §6.6 брифа, и знать это
 * раскладке неоткуда.
 *
 * Ветка ищется по узлу, а не по порядку: от одного коммита может уйти несколько веток, и тогда
 * акцент точке задаёт первая из них, а остальные получают свои собственные точки ветвления — по
 * одной на ветку.
 *
 * @param graphNode узел графа
 * @param own собственная ветка узла: ответ по умолчанию
 * @return ветка, чьи цвет и направление узел показывает
 */
internal fun Graph<GraphNode>.accentOwnerOf(graphNode: GraphNode, own: Branch.Id): Branch.Id {
  return when (graphNode) {
    is GraphNode.Fork -> branchForkedAt(graphNode.id) ?: own
    is GraphNode.Merge -> branchMergedAt(graphNode.id) ?: own
    is GraphNode.Episode,
    is GraphNode.Front -> own
  }
}
