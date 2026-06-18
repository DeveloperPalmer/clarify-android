package ru.sla.clarify.lib.google.firestore.entity.write
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Delete

/**
 * Отмена merge request'а: удаляет весь объект `mergeRequest` из документа ветки
 * через sentinel [Delete] (→ `FieldValue.delete()`). Имя свойства совпадает с
 * именем поля верхнего уровня, поэтому dotted-path не нужен.
 */
@Serializable
data class DeleteMergeRequestParams(
  @Contextual
  val mergeRequest: Delete
)
