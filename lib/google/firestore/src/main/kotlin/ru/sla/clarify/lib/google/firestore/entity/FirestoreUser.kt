package ru.sla.clarify.lib.google.firestore.entity

import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.UserId

data class FirestoreUser(
  val id: UserId,
  val email: Email?,
  val displayName: String?,
  val photoUrl: String?
)
