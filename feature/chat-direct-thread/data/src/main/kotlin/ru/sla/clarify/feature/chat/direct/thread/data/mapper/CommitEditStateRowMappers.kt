package ru.sla.clarify.feature.chat.direct.thread.data.mapper

import ru.sla.clarify.database.entity.CommitEditStateRow
import ru.sla.clarify.feature.chat.direct.thread.data.entity.EditState

internal fun CommitEditStateRow.toDomainModel(): EditState {
  return EditState(
    text = text,
    editedAtNanos = editedAtNanos,
    status = status
  )
}
