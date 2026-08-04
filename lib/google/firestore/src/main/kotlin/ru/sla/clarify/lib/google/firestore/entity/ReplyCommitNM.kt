package ru.sla.clarify.lib.google.firestore.entity

import kotlinx.serialization.Serializable

@Serializable
data class ReplyCommitNM(
  val id: String,
  val senderUid: String,
  val text: String
)
