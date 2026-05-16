package ru.sla.clarify.feature.entity.chat

import androidx.compose.runtime.Immutable

@Immutable
data class Peer(
  val id: Id,
  val faceUrl: String?
) {
  @Immutable
  data class Id(val value: String)
}
