package ru.sla.clarify.lib.google.firestore.entity

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationNM(
  val id: String,
  val type: Type,
  val memberUids: List<String> = emptyList(),
  val name: String? = null,
  val ownerUid: String? = null,
  val lastCommitText: String? = null,
  val lastCommitSenderUid: String? = null,
  @Contextual
  val lastCommitAt: Timestamp? = null
) {
  @Serializable
  enum class Type(val value: String) {
    @SerialName("direct")
    Direct("direct"),

    @SerialName("group")
    Group("group")
  }
}
