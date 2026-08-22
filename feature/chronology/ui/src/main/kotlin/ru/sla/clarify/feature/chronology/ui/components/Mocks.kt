package ru.sla.clarify.feature.chronology.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ru.sla.clarify.feature.chronology.ui.entity.MessageChipState

/**
 * Набор сообщений для превью [MessageChip].
 *
 * Каждое значение — отдельный кадр превью, а не ещё одна плашка в общей колонке: варианты, собранные
 * в одном кадре, скрывают, что именно сломалось, и растут в высоту с каждым новым состоянием.
 *
 * Последнее значение проверяет обрезку: текст заведомо длиннее [MessageChip] и обязан упереться в
 * максимальную ширину, а не растянуть плашку.
 */
@Immutable
internal class MessageChipPreviewProvider : PreviewParameterProvider<MessageChipPreview> {
  override val values = sequenceOf(
    MessageChipPreview(
      text = "Не бьётся по срокам",
      isMine = false
    ),
    MessageChipPreview(
      text = "Где именно?",
      isMine = true
    ),
    MessageChipPreview(
      text = "Выношу в ветку",
      isMine = true,
      state = MessageChipState.Edited
    ),
    MessageChipPreview(
      text = "Готово, ветка тут",
      isMine = false,
      state = MessageChipState.Quoted
    ),
    MessageChipPreview(
      text = "Фиксируем 14-е",
      isMine = true,
      state = MessageChipState.Sending
    ),
    MessageChipPreview(
      text = "Очень длинный текст сообщения, который обязан обрезаться эллипсисом",
      isMine = false
    )
  )
}

/**
 * Один кадр превью [MessageChip].
 *
 * @param text текст сообщения
 * @param isMine своё сообщение или собеседника
 * @param state состояние плашки: иконка и прозрачность
 */
@Immutable
internal data class MessageChipPreview(
  val text: String,
  val isMine: Boolean,
  val state: MessageChipState = MessageChipState.Normal
)
