package ru.sla.clarify.database.entity

data class CommitEditStateRow(
  val text: String,
  val editedAtNanos: Long?,
  val status: String
)
