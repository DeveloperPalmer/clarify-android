package ru.sla.clarify.feature.chat.conversation.domain.entity

import androidx.compose.runtime.Immutable

@Immutable
data class Participant(
  val id: Id,
  val displayName: String?,
  val photoUrl: String?
) {
  @JvmInline
  value class Id(val value: String)
}
