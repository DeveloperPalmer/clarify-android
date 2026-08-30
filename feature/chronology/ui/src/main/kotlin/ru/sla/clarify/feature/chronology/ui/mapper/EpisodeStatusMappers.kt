package ru.sla.clarify.feature.chronology.ui.mapper

import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chronology.ui.entity.Node

/**
 * Иконка, которой состояние сообщения показывает себя справа от текста.
 *
 * Состояния без иконки дают `null`, а не прозрачную заглушку: место под иконку занимать нечем, и
 * плашка обязана сжаться до текста.
 *
 * @return идентификатор рисунка или `null`, если состоянию нечего показывать
 */
internal fun Node.Episode.Status.toIconResId(): Int? {
  return when (this) {
    Node.Episode.Status.Normal,
    Node.Episode.Status.Sending -> null
    Node.Episode.Status.Edited -> R.drawable.ic_pencil_24
    // Иконки кавычек в core/resources нет — временно берём «ответить». Заведена в список к дизайнеру.
    Node.Episode.Status.Quoted -> R.drawable.ic_reply_24
  }
}
