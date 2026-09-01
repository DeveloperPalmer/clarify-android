package ru.sla.clarify.feature.chronology.ui.mapper

import ru.sla.atlas.entity.NodeDraft
import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeDraft
import ru.sla.clarify.feature.chronology.ui.entity.Node

/**
 * Черновик узла беседы — в черновик, из которого собирается граф.
 *
 * Теряется по дороге содержимое превью-карточки, и это не потеря: карточка — вторая поверхность
 * экрана, графу о ней знать незачем, а адресуется она идентификатором узла, который сборку
 * переживает.
 *
 * @return черновик с разрешением спора внутри одного момента
 */
internal fun GraphNodeDraft.toNodeDraft(): NodeDraft<Node> {
  return NodeDraft(
    node = node,
    branchId = branchId,
    at = at,
    order = node.toSortOrder()
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
private fun Node.toSortOrder(): Int {
  return when (this) {
    is Node.Episode -> 0
    is Node.Fork -> 1
    is Node.Merge -> 2
    is Node.Front -> 3
  }
}
