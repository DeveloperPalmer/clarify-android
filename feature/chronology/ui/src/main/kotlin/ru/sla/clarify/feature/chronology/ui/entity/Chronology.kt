package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.Node

/**
 * Хронология беседы целиком: граф, имена веток и их оттенки.
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
 * @param branchColors оттенок каждой ветки — по той же причине и с тем же уговором об отсутствии:
 *   у магистрали идентичности нет, и линия у неё нейтральная. Раньше это говорил нулевой номер
 *   оттенка внутри [Branch] — значение, которое обязаны были одинаково понимать все, кто его читает
 */
@Immutable
data class Chronology(
  val graph: Graph<GraphNode> = Graph.Empty,
  val branchNames: Map<Branch.Id, String> = emptyMap(),
  val branchColors: Map<Branch.Id, BranchColor> = emptyMap()
) {
  val previewById: Map<Node.Id, NodePreview> = graph.nodes
    .filterIsInstance<GraphNode.Episode>()
    .associate { it.id to it.preview }
}
