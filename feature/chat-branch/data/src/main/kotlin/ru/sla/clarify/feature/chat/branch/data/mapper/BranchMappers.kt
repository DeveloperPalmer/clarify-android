package ru.sla.clarify.feature.chat.branch.data.mapper

import ru.sla.clarify.database.chat.SelectEditState
import ru.sla.clarify.database.chat.SelectOldestCursor
import ru.sla.clarify.entity.chat.CommitCursor
import ru.sla.clarify.feature.chat.branch.data.entity.EditState

internal fun SelectOldestCursor.toCursor(): CommitCursor {
  return CommitCursor(
    id = id,
    createdAtNanos = createdAtNanos
  )
}

internal fun SelectEditState.toDomainModel(): EditState {
  return EditState(
    text = text,
    status = status,
    editedAtNanos = editedAtNanos
  )
}
