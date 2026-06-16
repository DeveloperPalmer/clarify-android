package ru.sla.clarify.lib.google.firestore.entity.write

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.codec.sentinel.ServerTimestamp
import ru.sla.clarify.lib.google.firestore.entity.CommitNM.Type

@Serializable
data class PostInviteMemberCommitParams(
  val id: String,
  @Contextual
  val senderUid: UserId,
  val invitedUid: String,
  val type: Type = Type.InviteMember,
  val branchId: String,
  @Contextual
  val createdAt: Timestamp,
  @Contextual
  val serverCreatedAt: ServerTimestamp = ServerTimestamp
)
