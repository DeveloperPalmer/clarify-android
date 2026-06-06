package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.entity.Approver
import ru.sla.clarify.feature.chat.thread.ui.entity.Commit

@Immutable
data class ViewState(
  val branchId: Branch.Id,
  val contentLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val branchName: String? = null,
  val mergeRequest: Branch.MergeRequest? = null,
  val mergeRequestRunning: Boolean = false,
  val initiatorName: String? = null,
  val approvers: List<Approver> = emptyList(),
  val currentUserId: UserId? = null,
  val commits: List<Commit> = emptyList(),
  val unreadCount: Int = 0
) {
  val isCurrentUserApproved: Boolean
    get() = mergeRequest != null &&
      currentUserId != null &&
      currentUserId in mergeRequest.approvedByIds
}
