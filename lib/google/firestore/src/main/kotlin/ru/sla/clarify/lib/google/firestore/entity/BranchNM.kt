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
 *
 * [lastCommitText]/[lastCommitAt] — денормализация последнего commit'а ветки (зеркало
 * `ConversationNM` для master-ветки): обновляются merge-записью при постинге commit'а.
 * `unreadCount` здесь НЕ живёт — он лежит в отдельном документе
 * `branches/{branchId}/unreadCommits/{uid}` и читается своим listener'ом.
 */
@Serializable
data class BranchNM(
  val id: String,
  val parentBranchId: String,
  val branchedFromCommitId: String,
  val name: String,
  val lastCommitText: String? = null,
  @Contextual
  val lastCommitAt: Timestamp? = null,
  val createdByUid: String,
  @Contextual
  val createdAt: Timestamp? = null,
  val mergeRequest: MergeRequestNM? = null
)
