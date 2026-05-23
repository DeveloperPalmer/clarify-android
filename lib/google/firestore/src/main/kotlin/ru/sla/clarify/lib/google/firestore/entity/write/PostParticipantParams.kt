package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Serializable

@Serializable
data class PostParticipantParams(
  val displayName: String? = null,
  val photoUrl: String? = null
)
