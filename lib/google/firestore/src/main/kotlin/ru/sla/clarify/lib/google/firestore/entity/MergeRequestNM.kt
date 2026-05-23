package ru.sla.clarify.lib.google.firestore.entity

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class MergeRequestNM(
  val initiatorUid: String,
  @Contextual
  val requestedAt: Timestamp,
  val approvedByUids: List<String> = emptyList()
)
