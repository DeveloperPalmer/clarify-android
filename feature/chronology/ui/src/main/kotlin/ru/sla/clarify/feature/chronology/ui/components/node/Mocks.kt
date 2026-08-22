package ru.sla.clarify.feature.chronology.ui.components.node

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ru.sla.clarify.feature.chronology.ui.entity.MessageNodeState

/**
 * Набор сообщений для превью [MessageNode].
 *
 * Каждое значение — отдельный кадр превью, а не ещё одна плашка в общей колонке: варианты, собранные
 * в одном кадре, скрывают, что именно сломалось, и растут в высоту с каждым новым состоянием.
 *
 * Последнее значение проверяет обрезку: текст заведомо длиннее [MessageNode] и обязан упереться в
 * максимальную ширину, а не растянуть плашку.
 */
@Immutable
internal class MessageNodePreviewProvider : PreviewParameterProvider<MessageNodePreview> {
  override val values = sequenceOf(
    MessageNodePreview(
      text = "Не бьётся по срокам",
      isMine = false
    ),
    MessageNodePreview(
      text = "Где именно?",
      isMine = true
    ),
    MessageNodePreview(
      text = "Выношу в ветку",
      isMine = true,
      state = MessageNodeState.Edited
    ),
    MessageNodePreview(
      text = "Готово, ветка тут",
      isMine = false,
      state = MessageNodeState.Quoted
    ),
    MessageNodePreview(
      text = "Фиксируем 14-е",
      isMine = true,
      state = MessageNodeState.Sending
    ),
    MessageNodePreview(
      text = "Очень длинный текст сообщения, который обязан обрезаться эллипсисом",
      isMine = false
    )
  )
}

/**
 * Один кадр превью [MessageNode].
 *
 * @param text текст сообщения
 * @param isMine своё сообщение или собеседника
 * @param state состояние плашки: иконка и прозрачность
 */
@Immutable
internal data class MessageNodePreview(
  val text: String,
  val isMine: Boolean,
  val state: MessageNodeState = MessageNodeState.Normal
)
