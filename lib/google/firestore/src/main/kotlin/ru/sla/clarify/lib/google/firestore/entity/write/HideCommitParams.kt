package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ArrayRemove

/**
 * Атомарное удаление текущего пользователя из `visibleFor` документа commit через
 * sentinel [ArrayRemove] (→ `FieldValue.arrayRemove(...)`) — «удалить сообщение только у себя».
 * Имя свойства совпадает с именем поля верхнего уровня, поэтому dotted-path не нужен.
 */
@Serializable
data class HideCommitParams(
  @Contextual
  val visibleFor: ArrayRemove
)
