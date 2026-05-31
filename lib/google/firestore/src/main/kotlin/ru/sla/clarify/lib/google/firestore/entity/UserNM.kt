package ru.sla.clarify.lib.google.firestore.entity

import kotlinx.serialization.Serializable

@Serializable
data class UserNM(
  val id: String,
  val email: String,
  val displayName: String,
  val photoUrl: String? = null
)
