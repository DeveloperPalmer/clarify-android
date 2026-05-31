package ru.sla.clarify.core.domain.entity

import androidx.compose.runtime.Immutable

@Immutable
data class User(
  val id: UserId,
  val email: Email,
  val displayName: String,
  val photoUrl: String?
)
