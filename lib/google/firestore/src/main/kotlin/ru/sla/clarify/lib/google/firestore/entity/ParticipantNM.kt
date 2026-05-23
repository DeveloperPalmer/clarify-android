package ru.sla.clarify.lib.google.firestore.entity

import kotlinx.serialization.Serializable

@Serializable
data class ParticipantNM(
  val id: String,
  val displayName: String? = null,
  val photoUrl: String? = null
)
