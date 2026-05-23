package ru.sla.clarify.lib.google.firestore.entity

import kotlinx.serialization.Serializable

@Serializable
data class UserNM(
  val id: String,
  val email: String? = null,
  val displayName: String? = null,
  val photoUrl: String? = null
)
