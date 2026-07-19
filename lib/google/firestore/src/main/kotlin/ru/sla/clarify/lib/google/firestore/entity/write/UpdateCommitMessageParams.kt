package ru.sla.clarify.lib.google.firestore.entity.write

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class UpdateCommitMessageParams(
  val text: String,
  @Contextual
  val editedAt: Timestamp
)
