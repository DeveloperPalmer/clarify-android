package ru.sla.atlas.entity

/**
 * Узел вызывающего в его наименьшем виде: только то, что обещает [BasicNode].
 *
 * Настоящий узел приносит с собой всё, что рисует, — и ничего из этого графу не нужно. Здесь
 * поэтому и лежит узел без единого собственного поля: он проверяет ровно то, на что граф вправе
 * рассчитывать.
 *
 * @param id идентификатор узла
 * @param gap пауза перед узлом; графу она безразлична, и потому у неё есть значение по умолчанию
 */
internal data class MockNode(
  override val id: BasicNode.Id,
  override val gap: TimeGap = TimeGap.Minutes
) : BasicNode

/**
 * Узлы подряд, с именами `n0`, `n1`, … — история, в которой важен только порядок.
 *
 * @param count сколько узлов в истории
 * @return узлы в порядке их индексов
 */
internal fun mockNodes(count: Int): List<MockNode> {
  return List(count) { index -> MockNode(id = BasicNode.Id("n$index")) }
}

/**
 * Ветка с её составом, перечисленным отрезком индексов истории.
 *
 * @param id идентификатор ветки
 * @param nodes отрезок собственных узлов; `null` — узлов у ветки нет вовсе
 * @param forkedFrom узел магистрали, от которого ветка ушла
 * @param mergedAt узел магистрали, в котором ветка слилась
 * @param status что с веткой происходит
 * @param colorIndex номер оттенка
 * @return ветка, готовая попасть в [Graph]
 */
internal fun mockBranch(
  id: String,
  nodes: IntRange? = null,
  forkedFrom: String? = null,
  mergedAt: String? = null,
  status: Branch.Status = Branch.Status.Alive,
  colorIndex: Int = 1
): Branch {
  return Branch(
    id = Branch.Id(id),
    nodeIds = nodes?.map { BasicNode.Id("n$it") }.orEmpty(),
    forkedFrom = forkedFrom?.let { BasicNode.Id(it) },
    mergedAt = mergedAt?.let { BasicNode.Id(it) },
    status = status,
    colorIndex = colorIndex
  )
}
