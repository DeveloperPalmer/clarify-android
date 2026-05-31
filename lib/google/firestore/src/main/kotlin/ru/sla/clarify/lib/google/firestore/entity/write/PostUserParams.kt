package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp

@Serializable
data class PostUserParams(
  val email: String,
  val displayName: String,
  val photoUrl: String? = null,
  @Contextual
  val createdAt: ServerTimestamp = ServerTimestamp,
  @Contextual
  val updatedAt: ServerTimestamp = ServerTimestamp
)
