package ru.sla.clarify.feature.entity.chat

import androidx.compose.runtime.Immutable

@Immutable
data class Participant(
  val id: Id,
  val displayName: String?,
  val photoUrl: String?
) {
  @Immutable
  data class Id(val value: String)
}
