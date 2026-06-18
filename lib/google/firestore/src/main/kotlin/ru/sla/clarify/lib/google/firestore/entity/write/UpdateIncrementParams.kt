package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Increment

/**
 * Атомарный инкремент `count` в `unreadCommits/{uid}`. Семантически отличается
 * от [UpdateUnreadCountParams] (reset-в-абсолют): тут sentinel-операция, value
 * — типизированно [Increment].
 */
@Serializable
data class UpdateIncrementParams(
  @Contextual
  val count: Increment
)
