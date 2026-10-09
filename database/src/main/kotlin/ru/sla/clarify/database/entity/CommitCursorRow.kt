package ru.sla.clarify.database.entity

import ru.sla.clarify.entity.chat.Commit

data class CommitCursorRow(
  val id: Commit.Id,
  val createdAtNanos: Long
)
