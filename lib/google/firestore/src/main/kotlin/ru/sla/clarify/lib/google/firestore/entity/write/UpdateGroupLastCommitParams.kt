package ru.sla.clarify.lib.google.firestore.entity.write

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp

@Serializable
data class UpdateGroupLastCommitParams(
  val lastCommitText: String,
  val lastCommitSenderUid: String,
  @Contextual
  val lastCommitAt: Timestamp,
  @Contextual
  val updatedAt: ServerTimestamp = ServerTimestamp
)
