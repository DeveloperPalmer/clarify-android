package ru.sla.clarify.feature.chat.conversation.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId

@Immutable
data class Group(
  val id: Conversation.Id,
  val name: String,
  val ownerId: UserId,
  val memberCount: Int,
  val participantIds: List<UserId>
)

@Immutable
data class GroupMember(
  val id: UserId,
  val displayName: String?,
  val photoUrl: String?,
  val isOwner: Boolean,
  val isMe: Boolean
)

@Immutable
data class FoundUser(
  val id: UserId,
  val displayName: String,
  val email: String,
  val photoUrl: String?
)
