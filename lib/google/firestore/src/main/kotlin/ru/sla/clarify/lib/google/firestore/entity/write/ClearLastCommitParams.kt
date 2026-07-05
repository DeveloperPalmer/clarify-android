package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Delete
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp

@Serializable
data class ClearLastCommitParams(
  @Contextual
  val lastCommitText: Delete,
  @Contextual
  val lastCommitSenderUid: Delete,
  @Contextual
  val lastCommitAt: Delete,
  @Contextual
  val updatedAt: ServerTimestamp = ServerTimestamp
)
