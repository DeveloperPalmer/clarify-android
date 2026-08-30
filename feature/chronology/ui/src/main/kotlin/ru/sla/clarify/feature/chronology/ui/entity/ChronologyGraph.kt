package ru.sla.clarify.feature.chronology.ui.entity

import androidx.compose.runtime.Immutable

/**
 * Граф беседы целиком: и раскладочная его часть, и то, что нарисовано внутри узлов.
 *
 * Одной сущностью, а не пятью полями состояния, и не случайно: занятость дорожек выводится из
 * индексов узлов по идентификаторам развилки и слияния, поэтому список веток, разъехавшийся с
 * узлами хотя бы на кадр, дал бы раскраску по чужим индексам — **молча**. Порознь эти списки
 * подменить невозможно по построению.
 *
 * @param nodes узлы в хронологическом порядке
 * @param branches ветки, кроме магистрали, в порядке ветвления — в этом же порядке идёт жадная
 *   раскраска дорожек
 * @param branchNames имя каждой ветки. Отдельной картой, а не полем [GraphBranch]: та — сущность
 *   раскладки, и имя в ней было бы содержимым, попавшим не в свой слой. Магистрали в карте нет, и
 *   это не пропуск: у неё нет темы, о которой можно сказать «ветка такая-то»
 * @param episodeById содержимое плашек по идентификатору узла; у точек на линии его нет
 * @param previewById содержимое превью-карточек; узел без него не нажимается вовсе
 */
@Immutable
data class ChronologyGraph(
  val nodes: List<GraphNode> = emptyList(),
  val branches: List<GraphBranch> = emptyList(),
  val branchNames: Map<GraphBranch.Id, String> = emptyMap(),
  val episodeById: Map<GraphNode.Id, EpisodeContent> = emptyMap(),
  val previewById: Map<GraphNode.Id, NodePreview> = emptyMap()
)
