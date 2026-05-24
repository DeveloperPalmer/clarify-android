package ru.sla.clarify.lib.google.firestore.entity

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Read-NM для `conversations/{cid}/branches/{branchId}`.
 *
 * Состояние merge-предложения целиком живёт внутри [mergeRequest]: его отсутствие
 * (== `null`) означает «ветка живая, принимает commit'ы». Финальный статус
 * (`Merged` с `mergedAt`/`mergedIntoBranchId`) — тоже поле этого объекта, не самой
 * ветки; так данные не дублируются между двумя сущностями и нет рассинхрона
 * статуса ветки vs статуса MR.
 */
@Serializable
data class BranchNM(
  val id: String,
  val parentBranchId: String,
  val branchedFromCommitId: String,
  val name: String,
  val createdByUid: String,
  @Contextual
  val createdAt: Timestamp? = null,
  val mergeRequest: MergeRequestNM? = null
)
