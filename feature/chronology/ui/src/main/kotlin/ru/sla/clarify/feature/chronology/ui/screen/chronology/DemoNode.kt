package ru.sla.clarify.feature.chronology.ui.screen.chronology

import androidx.compose.runtime.Immutable
import ru.sla.clarify.feature.chronology.ui.entity.GraphNode
import ru.sla.clarify.feature.chronology.ui.entity.MessageChipState

/**
 * Временный узел демо-графа: раскладочная часть плюс то, что нарисовано внутри плашки.
 *
 * Существует ровно до появления сборки графа из веток и коммитов — тогда содержимое узла придёт из
 * домена, а не из литералов экрана.
 *
 * @param node раскладочная часть: дорожка и пауза
 * @param text текст сообщения
 * @param isMine своё сообщение или собеседника
 * @param chipState состояние плашки: иконка и прозрачность
 */
@Immutable
data class DemoNode(
  val node: GraphNode,
  val text: String,
  val isMine: Boolean,
  val chipState: MessageChipState = MessageChipState.Normal
)
