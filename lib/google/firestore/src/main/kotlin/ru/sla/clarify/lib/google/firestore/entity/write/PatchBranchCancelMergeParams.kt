package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.codec.sentinel.Delete
import ru.sla.clarify.lib.google.firestore.entity.BranchNM.Status

@Serializable
data class PatchBranchCancelMergeParams(
  val status: Status,
  @Contextual
  val mergeRequest: Delete = Delete
)
