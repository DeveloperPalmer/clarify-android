package ru.sla.clarify.feature.chronology.ui.mapper

import ru.sla.atlas.entity.NodeDraft
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeDraft

/**
 * Черновик узла беседы — в черновик, из которого собирается граф.
 *
 * Теряется по дороге содержимое превью-карточки, и это не потеря: карточка — вторая поверхность
 * экрана, графу о ней знать незачем, а адресуется она идентификатором узла, который сборку
 * переживает.
 *
 * @return черновик с разрешением спора внутри одного момента
 */
internal fun GraphNodeDraft.toNodeDraft(): NodeDraft<GraphNode> {
  return NodeDraft(
    node = graphNode,
    branchId = branchId,
    at = at,
    order = graphNode.toSortOrder()
  )
}

/**
 * Порядок узлов, попавших на один и тот же момент.
 *
 * Совпадение это не экзотика, а обычное дело: фронт стоит на последнем сообщении магистрали, а
 * развилка — на том самом сообщении, от которого ушла ветка, и оно бывает первым в своём эпизоде.
 *
 * Порядок смысловой: сначала то, что было сказано, потом то, что из сказанного следует. Ветка ушла
 * **от** сообщения, значит после него; вернулась она позже, чем ушла; фронт замыкает магистраль.
 *
 * @return ключ сортировки внутри одного момента
 */
private fun GraphNode.toSortOrder(): Int {
  return when (this) {
    is GraphNode.Episode -> 0
    is GraphNode.Fork -> 1
    is GraphNode.Merge -> 2
    is GraphNode.Front -> 3
  }
}
