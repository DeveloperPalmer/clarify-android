package ru.sla.clarify.entity.chat

/**
 * Точный курсор пагинации по сообщениям: время создания плюс [id] как тай-брейк сортировки,
 * так что сообщения с одинаковым временем не пропадают и не задваиваются на стыке страниц.
 */
data class CommitCursor(
  val id: Commit.Id,
  val createdAtNanos: Long
)
