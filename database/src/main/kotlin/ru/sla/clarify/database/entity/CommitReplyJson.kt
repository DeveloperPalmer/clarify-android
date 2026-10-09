package ru.sla.clarify.database.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class CommitReplyJson(
  val id: String,
  val senderId: String,
  val isSelf: Boolean,
  val text: String
)
