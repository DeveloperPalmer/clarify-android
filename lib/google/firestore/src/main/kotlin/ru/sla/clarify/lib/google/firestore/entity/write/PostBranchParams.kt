package ru.sla.clarify.lib.google.firestore.entity.write

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.lib.google.firestore.entity.BranchNM.Status

@Serializable
data class PostBranchParams(
  val parentBranchId: String,
  val branchedFromCommitId: String,
  val name: String,
  val status: Status,
  @Contextual
  val createdAt: Timestamp,
  @Contextual
  val createdByUid: UserId
)
