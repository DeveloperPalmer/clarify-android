package ru.sla.clarify.feature.chat.branch.data.mapper

import ru.sla.clarify.database.chat.SelectEditState
import ru.sla.clarify.database.chat.SelectOldestCursor
import ru.sla.clarify.feature.chat.branch.data.entity.EditState
import ru.sla.clarify.lib.google.firestore.entity.CommitCursor
import ru.sla.clarify.lib.google.firestore.epochNanosToTimestamp

internal fun SelectOldestCursor.toCursor(): CommitCursor {
  return CommitCursor(
    id = id.value,
    createdAt = createdAtNanos.epochNanosToTimestamp()
  )
}

internal fun SelectEditState.toDomainModel(): EditState {
  return EditState(
    text = text,
    status = status,
    editedAtNanos = editedAtNanos
  )
}
