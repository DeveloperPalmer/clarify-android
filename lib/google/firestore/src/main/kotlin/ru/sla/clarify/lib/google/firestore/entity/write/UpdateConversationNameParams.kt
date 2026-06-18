package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Serializable

@Serializable
data class UpdateConversationNameParams(
  val name: String
)
