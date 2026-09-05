package ru.sla.clarify.app.data.channel

import ru.sla.clarify.app.data.entity.DecodedFrame

/**
 * Решение по кадру — чистая функция от текущего курсора и самого кадра.
 *
 * Отдельной ветки «если первый запуск» здесь нет намеренно: `current = 0` — самое частое
 * состояние на новом устройстве, и оно проходит той же дорогой, что и просроченный курсор.
 * Заведи мы отдельную ветку, она разошлась бы с основной на первой же правке — а поймать это
 * можно только на новом устройстве, то есть позже всего.
 */
internal fun decideCursor(current: Long, frame: DecodedFrame<*>): CursorDecision {
  if (frame.resyncRequired) {
    // Сервер не сказал, откуда идёт лента, — начинаем с нуля: лучше лишний догон, чем дыра
    return CursorDecision.Resync(fromSeq = frame.resyncFromSeq ?: 0L)
  }
  val firstSeq = frame.firstSeq ?: return CursorDecision.Advance(current)
  if (firstSeq != current + 1) {
    // Разрыв: между тем, что у нас есть, и тем, что прислали, потерялись события. Пропустить их
    // молча значит навсегда остаться с дырой в ленте, которую уже ничем не заполнить.
    return CursorDecision.Resync(fromSeq = frame.lastSeq ?: current)
  }
  return CursorDecision.Advance(frame.lastSeq ?: current)
}
