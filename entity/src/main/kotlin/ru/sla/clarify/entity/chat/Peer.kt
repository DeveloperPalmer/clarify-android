package ru.sla.clarify.entity.chat

import androidx.compose.runtime.Immutable

@Immutable
data class Peer(
  val id: Id,
  val displayName: String,
  val photoUrl: String?
) {
  @Immutable
  data class Id(val value: String)
}
