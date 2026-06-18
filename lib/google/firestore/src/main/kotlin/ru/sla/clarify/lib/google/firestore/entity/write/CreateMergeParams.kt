package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.entity.MergeRequestNM

/**
 * Открытие merge request'а. Записываем только подобъект `mergeRequest` — статус
 * самой ветки в новой схеме не существует, всё состояние merge'а живёт в этом
 * вложенном объекте.
 */
@Serializable
data class CreateMergeParams(
  val mergeRequest: MergeRequestNM
)
