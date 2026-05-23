package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Increment

/**
 * Атомарный инкремент `count` в `unreadCommits/{uid}`. Семантически отличается
 * от [PatchUnreadCountParams] (reset-в-абсолют): тут sentinel-операция, value
 * — типизированно [Increment].
 */
@Serializable
data class PatchUnreadIncrementParams(
  @Contextual
  val count: Increment = Increment(1)
)
