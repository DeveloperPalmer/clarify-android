package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.core.domain.entity.UserId

@Serializable
data class ReplyCommit(
  val id: String,
  @Contextual
  val senderUid: UserId,
  val text: String
)
