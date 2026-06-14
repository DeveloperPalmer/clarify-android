package ru.sla.clarify.feature.chat.group.thread.domain.entity

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId

@Immutable
data class FoundUser(
  val id: UserId,
  val displayName: String,
  val email: String,
  val photoUrl: String?
)
