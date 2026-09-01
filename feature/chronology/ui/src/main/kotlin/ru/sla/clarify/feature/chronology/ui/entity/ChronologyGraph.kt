package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.Node

/**
 * Граф беседы целиком: раскладка, имена веток и содержимое превью-карточек.
 *
 * Одной сущностью, а не отдельными полями состояния: и то, и другое адресуется идентификаторами
 * узлов, и карта, разъехавшаяся с графом хотя бы на кадр, дала бы карточку с чужим текстом —
 * **молча**. Что порознь нельзя подменить сами узлы и ветки, стережёт уже [Graph].
 *
 * @param layout раскладка: порядок узлов и состав веток. Узел здесь свой, [GraphNode], а не базовый:
 *   всё, что плашка рисует, лежит внутри него
 * @param branchNames имя каждой ветки. Отдельной картой, а не полем [Branch]: та — сущность
 *   раскладки, и имя в ней было бы содержимым, попавшим не в свой слой. Магистрали в карте нет, и
 *   это не пропуск: у неё нет темы, о которой можно сказать «ветка такая-то»
 * @param previewById содержимое превью-карточек; узел без него не нажимается вовсе. В узел не
 *   убрано намеренно: карточка — вторая поверхность, узел её не рисует и о ней не знает
 */
@Immutable
data class ChronologyGraph(
  val layout: Graph<GraphNode> = Graph.Empty,
  val branchNames: Map<Branch.Id, String> = emptyMap(),
  val previewById: Map<Node.Id, NodePreview> = emptyMap()
)
