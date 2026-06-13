package ru.sla.clarify.feature.chat.direct.thread.ui.entity

import androidx.compose.runtime.Immutable

@Immutable
data class GroupMember(
  val id: String,
  val displayName: String,
  val email: String,
  val photoUrl: String?,
  val isOwner: Boolean,
  val isMe: Boolean
)
