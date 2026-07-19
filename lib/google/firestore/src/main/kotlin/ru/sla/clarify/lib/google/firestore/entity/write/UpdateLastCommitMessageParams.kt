package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp

@Serializable
data class UpdateLastCommitMessageParams(
  val lastCommitText: String,
  @Contextual
  val updatedAt: ServerTimestamp = ServerTimestamp
)
