package ru.sla.clarify.lib.google.firestore.entity

import ru.sla.clarify.core.domain.entity.UserId

data class FirestoreParticipant(
  val id: UserId,
  val displayName: String?,
  val photoUrl: String?
)
