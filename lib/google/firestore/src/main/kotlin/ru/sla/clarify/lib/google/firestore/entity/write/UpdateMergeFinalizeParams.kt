package ru.sla.clarify.lib.google.firestore.entity.write

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BRANCH_MERGE_REQUEST
import ru.sla.clarify.lib.google.firestore.entity.MergeRequestNM

/**
 * Финализация merge'а: статус → [MergeRequestNM.Status.Merged] плюс простановка
 * `mergedAt`/`mergedIntoBranchId`. Dotted field-path'ы — частичное обновление
 * вложенного `mergeRequest` без его перезаписи.
 */
@Serializable
data class UpdateMergeFinalizeParams(
  @Contextual
  @SerialName("$BRANCH_MERGE_REQUEST.mergedAt")
  val mergedAt: Timestamp,
  @SerialName("$BRANCH_MERGE_REQUEST.mergedIntoBranchId")
  val mergedIntoBranchId: String,
  @SerialName("$BRANCH_MERGE_REQUEST.status")
  val status: MergeRequestNM.Status
)
