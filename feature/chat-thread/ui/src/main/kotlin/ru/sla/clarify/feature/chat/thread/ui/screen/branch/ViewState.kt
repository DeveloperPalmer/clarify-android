package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit

@Immutable
data class ViewState(
  val branchId: Branch.Id,
  val contentLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val branchName: String? = null,
  val branchStatus: Branch.Status = Branch.Status.Active,
  val mergeRequest: Branch.MergeRequest? = null,
  val initiatorName: String? = null,
  val currentUserId: UserId? = null,
  val commits: List<Commit> = emptyList(),
  val isSending: Boolean = false,
  /**
   * true пока хоть одна merge-задача (request/approve/revoke/cancel) в полёте.
   * UI использует это чтобы заблокировать кнопки и показать индикатор прогресса.
   */
  val isMergeActionPending: Boolean = false
) {
  /** Хелпер: является ли текущий пользователь инициатором merge request'а. */
  val isCurrentUserInitiator: Boolean
    get() = mergeRequest != null && currentUserId != null && mergeRequest.initiatorUid == currentUserId

  /** Хелпер: одобрил ли текущий пользователь активный merge. */
  val isCurrentUserApprover: Boolean
    get() = mergeRequest != null && currentUserId != null && currentUserId in mergeRequest.approvedByUids
}
