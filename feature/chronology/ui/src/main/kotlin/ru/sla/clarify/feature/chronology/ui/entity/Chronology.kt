package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.Node

/**
 * Хронология беседы целиком: граф и имена веток.
 *
 * Одной сущностью, а не отдельными полями состояния: имя адресуется тождеством ветки, и карта,
 * разъехавшаяся с графом хотя бы на кадр, назвала бы ветку чужой темой — **молча**. Что порознь
 * нельзя подменить сами узлы и ветки, стережёт уже [Graph].
 *
 * @param graph порядок узлов и состав веток. Узел здесь свой, [GraphNode], а не базовый: всё, что
 *   плашка рисует и что разворачивает карточка, лежит внутри него
 * @param branchNames имя каждой ветки. Отдельной картой, а не полем [Branch]: та — сущность графа,
 *   и имя в ней было бы содержимым, попавшим не в свой слой. Магистрали в карте нет, и это не
 *   пропуск: у неё нет темы, о которой можно сказать «ветка такая-то»
 */
@Immutable
data class Chronology(
  val graph: Graph<GraphNode> = Graph.Empty,
  val branchNames: Map<Branch.Id, String> = emptyMap()
) {
  val previewById: Map<Node.Id, NodePreview> = graph.nodes
    .filterIsInstance<GraphNode.Episode>()
    .associate { it.id to it.preview }
}
