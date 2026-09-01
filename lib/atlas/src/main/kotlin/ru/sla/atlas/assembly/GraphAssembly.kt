package ru.sla.atlas.assembly

import ru.sla.atlas.entity.BasicNode
import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.Graph
import ru.sla.atlas.entity.NodeDraft
import ru.sla.atlas.entity.TimeGap
import java.time.Duration

/**
 * Собирает граф из черновиков: расставляет узлы по времени, проставляет паузы и раздаёт веткам их
 * состав.
 *
 * **Собирает граф тот, кто проверяет его состав.** [Graph] требует, чтобы каждый узел принадлежал
 * ровно одной ветке, и ловит нарушение в конструкторе — а собирали граф до сих пор этажом выше, где
 * этой проверки нет. Три величины, которые там сходились руками, сходятся здесь: порядок узлов,
 * паузы между ними и обратная сторона порядка — состав каждой ветки.
 *
 * **Порядок задаёт время, а не ветка.** Ось X накапливается по порядку списка, поэтому набор,
 * сгруппированный по веткам, поставил бы позднюю ветку левее ранней. Узлы всех веток сводятся в
 * один список и сортируются один раз, и только после этого у каждого появляется пауза: она
 * считается от соседа **по времени**, а кто сосед, до сортировки неизвестно.
 *
 * **Пауза меряется по соседу в списке, а не по соседу той же ветки.** Зазор раздвигает узлы по
 * общей оси, и пауза внутри ветки, посчитанная в обход чужих узлов, поставила бы её узлы поверх них.
 *
 * Перед первым узлом паузы нет: отступ от края полотна дают поля, а не выдуманный зазор.
 *
 * @param N узел вызывающего
 * @param drafts узлы в любом порядке: в нужный их поставит сортировка
 * @param baseline магистраль без состава: состав ей раздаст сборка
 * @param branches остальные ветки, тоже без состава
 * @param gapOf чем меряется пауза: длительность в ступень зазора. Ступени — свойство вызывающего,
 *   и одинаковых у разных полотен не бывает
 * @param withGap тот же узел с проставленной паузой; идентификатор при этом обязан сохраниться —
 *   по нему ветки и находят свои узлы
 * @return граф, прошедший проверку состава
 */
fun <N : BasicNode> graphOf(
  drafts: List<NodeDraft<N>>,
  baseline: Branch,
  branches: List<Branch>,
  gapOf: (Duration) -> TimeGap,
  withGap: (node: N, gap: TimeGap) -> N
): Graph<N> {
  val ordered = drafts.sortedWith(compareBy({ it.at }, { it.order }))
  val nodes = ordered.mapIndexed { index, draft ->
    val previous = ordered.getOrNull(index - 1)
    withGap(draft.node, gapOf(Duration.between(previous?.at ?: draft.at, draft.at)))
  }
  val nodeIdsByBranch = ordered.groupBy({ it.branchId }, { it.node.id })
  return Graph(
    nodes = nodes,
    baseline = baseline.copy(nodeIds = nodeIdsByBranch[baseline.id].orEmpty()),
    branches = branches.map { branch -> branch.copy(nodeIds = nodeIdsByBranch[branch.id].orEmpty()) }
  )
}
