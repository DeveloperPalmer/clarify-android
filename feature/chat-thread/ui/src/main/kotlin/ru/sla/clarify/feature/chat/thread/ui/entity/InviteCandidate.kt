package ru.sla.clarify.feature.chat.thread.ui.entity

import androidx.compose.runtime.Immutable

@Immutable
data class InviteCandidate(
  val id: String,
  val displayName: String,
  val email: String,
  val photoUrl: String?,
  val isAlreadyMember: Boolean,
  val isSelected: Boolean
)
