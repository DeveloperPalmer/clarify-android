package ru.sla.clarify.lib.google.firestore.entity

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BranchNM(
  val id: String,
  val parentBranchId: String,
  val branchedFromCommitId: String,
  val name: String,
  val status: Status,
  val createdByUid: String,
  @Contextual
  val createdAt: Timestamp? = null,
  val mergeRequest: MergeRequestNM? = null,
  @Contextual
  val mergedAt: Timestamp? = null,
  val mergedIntoBranchId: String? = null
) {

  @Serializable
  enum class Status(val value: String) {
    @SerialName("active")
    Active("active"),

    @SerialName("mergeInProgress")
    MergeInProgress("mergeInProgress"),

    @SerialName("merged")
    Merged("merged")
  }
}
