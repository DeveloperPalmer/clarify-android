package ru.sla.clarify.feature.chronology.ui.mapper

import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.entity.MessageNodeState

/**
 * Иконка, которой состояние сообщения показывает себя справа от текста.
 *
 * Состояния без иконки дают `null`, а не прозрачную заглушку: место под иконку занимать нечем, и
 * плашка обязана сжаться до текста.
 *
 * @return идентификатор рисунка или `null`, если состоянию нечего показывать
 */
internal fun MessageNodeState.toIconResId(): Int? {
  return when (this) {
    MessageNodeState.Normal,
    MessageNodeState.Sending -> null
    MessageNodeState.Edited -> R.drawable.ic_pencil_24
    // Иконки кавычек в core/resources нет — временно берём «ответить». Заведена в список к дизайнеру.
    MessageNodeState.Quoted -> R.drawable.ic_reply_24
  }
}
