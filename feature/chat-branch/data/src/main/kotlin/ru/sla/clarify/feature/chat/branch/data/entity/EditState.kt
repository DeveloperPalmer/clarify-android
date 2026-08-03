package ru.sla.clarify.feature.chat.branch.data.entity

/** Снимок редактируемых полей строки кэша для отката оптимистичной правки при ошибке записи. */
internal data class EditState(
  val text: String,
  val editedAtNanos: Long?,
  val status: String
)
