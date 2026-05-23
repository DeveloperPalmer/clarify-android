package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp

/**
 * Частичный апдейт `users/{uid}` через `set(merge)`. Все payload-поля nullable с
 * `@EncodeDefault(NEVER)` — если значение `null`, оно не попадает в map'у запроса
 * и существующее в документе не перезатирается. `updatedAt` всегда пишется
 * server-stamp'ом.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PatchUserParams(
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  val displayName: String? = null,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  val photoUrl: String? = null,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  val email: String? = null,
  @Contextual
  val updatedAt: ServerTimestamp = ServerTimestamp
)
