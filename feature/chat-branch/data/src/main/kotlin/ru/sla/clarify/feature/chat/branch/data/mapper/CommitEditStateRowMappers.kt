package ru.sla.clarify.feature.chat.branch.data.mapper

import ru.sla.clarify.database.entity.CommitEditStateRow
import ru.sla.clarify.feature.chat.branch.data.entity.EditState

internal fun CommitEditStateRow.toDomainModel(): EditState {
  return EditState(
    text = text,
    status = status,
    editedAtNanos = editedAtNanos
  )
}
