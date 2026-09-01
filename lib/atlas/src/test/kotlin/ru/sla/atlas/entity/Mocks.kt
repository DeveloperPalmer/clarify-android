package ru.sla.atlas.entity

import androidx.compose.ui.graphics.Color

/**
 * Узел вызывающего в его наименьшем виде: только то, что обещает [Node].
 *
 * Настоящий узел приносит с собой всё, что рисует, — и ничего из этого графу не нужно. Здесь
 * поэтому и лежит узел без единого собственного поля: он проверяет ровно то, на что граф вправе
 * рассчитывать.
 *
 * @param id идентификатор узла
 * @param gap пауза перед узлом; графу она безразлична, и потому у неё есть значение по умолчанию
 */
internal data class MockNode(
  override val id: Node.Id,
  override val gap: TimeGap = TimeGap.Minutes
) : Node

/**
 * Узел с заданным именем.
 *
 * @param id идентификатор узла
 * @return узел без единого собственного поля
 */
internal fun mockNode(id: String): MockNode {
  return MockNode(id = Node.Id(id))
}

/**
 * Узлы подряд, с именами `n0`, `n1`, … — история, в которой важен только порядок.
 *
 * @param count сколько узлов в истории
 * @return узлы в порядке их индексов
 */
internal fun mockNodes(count: Int): List<MockNode> {
  return List(count) { index -> mockNode("n$index") }
}

/**
 * Ветка с её составом, перечисленным именами узлов.
 *
 * Состав задаётся именами, а не отрезком индексов: узлы ветки не обязаны идти подряд, и отрезок
 * подсказывал бы обратное там, где как раз проверяется, что ветка держит дорожку и в промежутках.
 *
 * @param id идентификатор ветки
 * @param nodes имена собственных узлов; пусто — узлов у ветки нет вовсе
 * @param forkedFrom узел магистрали, от которого ветка ушла
 * @param mergedAt узел магистрали, в котором ветка слилась
 * @param status что с веткой происходит
 * @param colorIndex номер оттенка
 * @return ветка, готовая попасть в [Graph]
 */
internal fun mockBranch(
  id: String,
  nodes: List<String> = emptyList(),
  forkedFrom: String? = null,
  mergedAt: String? = null,
  status: Branch.Status = Branch.Status.Alive,
  colorIndex: Int = 1
): Branch {
  return Branch(
    id = Branch.Id(id),
    nodeIds = nodes.map { Node.Id(it) },
    forkedFrom = forkedFrom?.let { Node.Id(it) },
    mergedAt = mergedAt?.let { Node.Id(it) },
    status = status,
    colorIndex = colorIndex
  )
}

/**
 * Имена узлов истории по их индексам: `3..5` — это `n3`, `n4`, `n5`.
 *
 * @param range отрезок индексов
 * @return имена узлов в порядке индексов
 */
internal fun mockNodeNames(range: IntRange): List<String> {
  return range.map { "n$it" }
}

/**
 * Цвет ветки вместо палитры темы.
 *
 * Настоящий цвет приходит от вызывающего, которого в юнит-тесте нет и заводить незачем: раскладка
 * цвет не толкует, а только переносит из ветки в ребро и в засечку. Здесь поэтому важно одно —
 * чтобы у веток с разными оттенками цвета были разные, а у веток с одним оттенком одинаковые.
 *
 * @param colorIndex номер оттенка ветки
 * @return цвет, однозначно соответствующий номеру
 */
internal fun mockLaneColor(colorIndex: Int): Color {
  return Color(red = colorIndex * 20, green = 0, blue = 0)
}

/**
 * Цвета всех веток графа: то, что настоящему полотну отдаёт вызывающий.
 *
 * @return цвет каждой ветки графа, магистраль включая
 */
internal fun Graph<Node>.mockBranchColors(): Map<Branch.Id, Color> {
  return (listOf(baseline) + branches).associate { it.id to mockLaneColor(it.colorIndex) }
}
