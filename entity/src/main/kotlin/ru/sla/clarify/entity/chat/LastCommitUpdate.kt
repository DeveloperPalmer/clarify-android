package ru.sla.clarify.entity.chat

import ru.sla.clarify.core.domain.entity.UserId
import java.time.LocalDateTime

/**
 * Что сделать с денормализованным превью последнего сообщения после удаления:
 *
 * - [Keep]    — удалили не последнее, превью не трогаем;
 * - [Replace] — удалили последнее, превью переезжает на новое последнее оставшееся;
 * - [Clear]   — сообщений не осталось.
 */
sealed interface LastCommitUpdate {

  data object Keep : LastCommitUpdate

  data object Clear : LastCommitUpdate

  data class Replace(
    val text: String,
    val senderId: UserId,
    val at: LocalDateTime
  ) : LastCommitUpdate
}
