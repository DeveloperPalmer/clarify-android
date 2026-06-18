package ru.sla.clarify.lib.google.firestore.entity.write

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.BRANCH_MERGE_REQUEST
import ru.sla.clarify.lib.google.firestore.entity.MergeRequestNM

/**
 * Частичное обновление вложенного `mergeRequest`: список approve'ов и статус.
 * Поля адресуются dotted field-path'ами, чтобы Firestore `update()` правил только
 * их, а не перезаписывал весь объект `mergeRequest`.
 */
@Serializable
data class UpdateMergeApprovalParams(
  @SerialName("$BRANCH_MERGE_REQUEST.approvedByUids")
  val approvedByUids: List<String>,
  @SerialName("$BRANCH_MERGE_REQUEST.status")
  val status: MergeRequestNM.Status
)
