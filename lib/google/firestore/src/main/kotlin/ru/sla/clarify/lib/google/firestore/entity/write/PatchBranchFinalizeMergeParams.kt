package ru.sla.clarify.lib.google.firestore.entity.write

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Delete
import ru.sla.clarify.lib.google.firestore.entity.BranchNM.Status

@Serializable
data class PatchBranchFinalizeMergeParams(
  val status: Status,
  @Contextual
  val mergedAt: Timestamp,
  val mergedIntoBranchId: String,
  @Contextual
  val mergeRequest: Delete = Delete
)
