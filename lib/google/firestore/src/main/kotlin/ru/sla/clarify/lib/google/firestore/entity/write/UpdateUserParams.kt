package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp

/**
 * Частичный апдейт `users/{uid}` через `set(merge)`. `displayName` и `email`
 * пишутся всегда. Nullable-поля с `@EncodeDefault(NEVER)` — если значение `null`,
 * оно не попадает в map'у запроса и существующее в документе не перезатирается.
 * `updatedAt` всегда пишется server-stamp'ом.
 */
@Serializable
data class UpdateUserParams(
  val email: String,
  val displayName: String,
  @EncodeDefault(EncodeDefault.Mode.NEVER)
  val photoUrl: String? = null,
  @Contextual
  val updatedAt: ServerTimestamp = ServerTimestamp
)
