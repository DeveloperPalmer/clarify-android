package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.entity.BranchNM.Status
import ru.sla.clarify.lib.google.firestore.entity.MergeRequestNM

@Serializable
data class PatchBranchOpenMergeParams(
  val status: Status,
  val mergeRequest: MergeRequestNM
)
