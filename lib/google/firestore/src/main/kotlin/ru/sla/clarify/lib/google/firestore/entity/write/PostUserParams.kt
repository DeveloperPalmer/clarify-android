package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp

@Serializable
data class PostUserParams(
  val displayName: String? = null,
  val photoUrl: String? = null,
  val email: String? = null,
  @Contextual
  val createdAt: ServerTimestamp = ServerTimestamp,
  @Contextual
  val updatedAt: ServerTimestamp = ServerTimestamp
)
