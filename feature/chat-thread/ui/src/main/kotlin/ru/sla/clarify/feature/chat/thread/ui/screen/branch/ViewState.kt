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
  val branchName: String = "",
  val branchStatus: Branch.Status = Branch.Status.Active,
  val mergeRequest: Branch.MergeRequest? = null,
  val currentUserId: UserId? = null,
  val commits: List<Commit> = emptyList(),
  val isSending: Boolean = false,
  /**
   * True while any of the merge-approval tasks (request/approve/revoke/cancel) is in flight.
   * UI uses it to disable buttons and surface a progress indicator.
   */
  val isMergeActionPending: Boolean = false
) {
  /** Convenience: is the current user the one who started the merge request? */
  val isCurrentUserInitiator: Boolean
    get() = mergeRequest != null && currentUserId != null && mergeRequest.initiatorUid == currentUserId

  /** Convenience: did the current user already approve the in-flight merge? */
  val isCurrentUserApprover: Boolean
    get() = mergeRequest != null && currentUserId != null && currentUserId in mergeRequest.approvedByUids
}
