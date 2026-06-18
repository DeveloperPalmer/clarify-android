package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ArrayRemove

/**
 * Атомарное удаление участников из `memberUids` документа conversation через
 * sentinel [ArrayRemove] (→ `FieldValue.arrayRemove(...)`). Имя свойства совпадает
 * с именем поля верхнего уровня, поэтому dotted-path не нужен.
 */
@Serializable
data class DeleteConversationMemberParams(
  @Contextual
  val memberUids: ArrayRemove
)
