package ru.sla.clarify.lib.google.firestore.entity

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ParticipantNM(
  val id: String,
  @Contextual
  val lastReadAt: Timestamp? = null
)
