package ru.sla.clarify.lib.google.firestore.entity

data class FirestoreListenerRunawayException(
  override val message: String
) : RuntimeException(message)
