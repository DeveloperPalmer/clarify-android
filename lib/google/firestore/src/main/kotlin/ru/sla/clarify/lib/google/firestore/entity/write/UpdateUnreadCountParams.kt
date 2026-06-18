package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Serializable

/**
 * Сброс счётчика непрочитанного в абсолютное значение (обычно `0`) через
 * `set(merge)`. Это не sentinel-операция и не удаление поля — именно перезапись
 * `count`. Атомарный инкремент того же поля живёт в [UpdateIncrementParams].
 */
@Serializable
data class UpdateUnreadCountParams(
  val count: Long
)
