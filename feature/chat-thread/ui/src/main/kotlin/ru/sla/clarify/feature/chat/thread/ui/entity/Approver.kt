package ru.sla.clarify.feature.chat.thread.ui.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId

@Immutable
data class Approver(
  val userId: UserId,
  val displayName: String?,
  val photoUrl: String?,
  val isApproved: Boolean
)
