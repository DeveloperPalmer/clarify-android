package ru.sla.clarify.mapper.data

import ru.sla.clarify.database.entity.CommitCursorRow
import ru.sla.clarify.entity.chat.CommitCursor

fun CommitCursorRow.toDomainModel(): CommitCursor {
  return CommitCursor(
    id = id,
    createdAtNanos = createdAtNanos
  )
}
