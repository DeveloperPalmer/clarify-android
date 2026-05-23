package ru.sla.clarify.lib.google.firestore.entity

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommitNM(
  val id: String,
  val senderUid: String,
  val branchId: String,
  val colorHex: String,
  val text: String? = null,
  @Contextual
  val createdAt: Timestamp? = null
) {
  @Serializable
  enum class Type(val value: String) {
    @SerialName("text")
    Text("text")
  }
}
