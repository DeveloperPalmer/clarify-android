package ru.sla.clarify.feature.entity.chat

import androidx.compose.runtime.Immutable

@Immutable
data class Participant(
  val id: Id
) {
  @Immutable
  data class Id(val value: String)
}
