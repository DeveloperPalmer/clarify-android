package ru.sla.atlas.mapper

import ru.sla.atlas.entity.Branch
import ru.sla.atlas.entity.BranchDraft
import ru.sla.atlas.entity.Node

/**
 * Черновик ветки — в ветку графа, когда её состав уже посчитан.
 *
 * Состав приходит параметром, а не берётся у черновика: у него этого знания нет по построению, и
 * единственный, у кого оно есть, — сборка, сводящая узлы всех веток в один порядок.
 *
 * @param nodeIds узлы ветки в порядке графа; пусто — узлов у ветки нет вовсе
 * @return ветка, готовая попасть в граф
 */
internal fun BranchDraft.toBranch(nodeIds: List<Node.Id>): Branch {
  return Branch(
    id = id,
    nodeIds = nodeIds,
    forkedFrom = forkedFrom,
    mergedAt = mergedAt,
    status = status
  )
}
