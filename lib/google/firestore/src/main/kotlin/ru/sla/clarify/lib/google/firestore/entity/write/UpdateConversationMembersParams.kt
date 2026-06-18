package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ArrayUnion

/**
 * Атомарное добавление участников в `memberUids` документа conversation через
 * sentinel [ArrayUnion] (→ `FieldValue.arrayUnion(...)`). Имя свойства совпадает
 * с именем поля верхнего уровня, поэтому dotted-path не нужен.
 */
@Serializable
data class UpdateConversationMembersParams(
  @Contextual
  val memberUids: ArrayUnion
)
