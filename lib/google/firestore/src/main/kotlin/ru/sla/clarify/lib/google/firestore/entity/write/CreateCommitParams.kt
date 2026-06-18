package ru.sla.clarify.lib.google.firestore.entity.write

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp
import ru.sla.clarify.lib.google.firestore.entity.CommitNM.Type

@Serializable
data class CreateCommitParams(
  @Contextual
  val senderUid: UserId,
  val text: String,
  val type: Type,
  @Contextual
  val createdAt: Timestamp,
  val branchId: String,
  @Contextual
  val serverCreatedAt: ServerTimestamp = ServerTimestamp
)
