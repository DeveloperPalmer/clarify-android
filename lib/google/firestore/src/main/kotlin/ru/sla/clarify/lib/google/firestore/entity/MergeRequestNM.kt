package ru.sla.clarify.lib.google.firestore.entity

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Read-NM для объекта `mergeRequest` внутри документа ветки. Workflow:
 *
 *  - [Status.Open]         — открыт, идёт сбор approvals.
 *  - [Status.ReadyToMerge] — все участники approved, ждём явный finalize.
 *  - [Status.Merged]       — финализирован; [mergedAt] и [mergedIntoBranchId]
 *                            заполнены, ветка больше не принимает commit'ы.
 *
 * Инициатор НЕ добавляется в [approvedByUids] автоматически — он должен явно
 * нажать approve, как и остальные участники.
 */
@Serializable
data class MergeRequestNM(
  val status: Status,
  val initiatorUid: String,
  @Contextual
  val requestedAt: Timestamp,
  val approvedByUids: List<String> = emptyList(),
  @Contextual
  val mergedAt: Timestamp? = null,
  val mergedIntoBranchId: String? = null
) {

  @Serializable
  enum class Status(val value: String) {
    @SerialName("open")
    Open("open"),

    @SerialName("readyToMerge")
    ReadyToMerge("readyToMerge"),

    @SerialName("merged")
    Merged("merged")
  }
}
