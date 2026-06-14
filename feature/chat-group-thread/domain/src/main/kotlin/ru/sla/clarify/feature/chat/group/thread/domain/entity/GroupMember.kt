package ru.sla.clarify.feature.chat.group.thread.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId

@Immutable
data class GroupMember(
  val id: UserId,
  val displayName: String?,
  val email: String?,
  val photoUrl: String?,
  val isOwner: Boolean,
  val isMe: Boolean
)
