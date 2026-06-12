package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Serializable

@Serializable
data class PatchConversationNameParams(
  val name: String
)
