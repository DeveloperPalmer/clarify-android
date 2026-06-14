package ru.sla.clarify.feature.chat.branch.ui.screen

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chat.branch.ui.entity.Approver
import ru.sla.clarify.feature.chat.branch.ui.entity.Commit
import ru.sla.clarify.feature.entity.chat.Branch
import ru.sla.clarify.feature.entity.chat.Member

@Immutable
data class ViewState(
  val branchId: Branch.Id,
  val contentLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val branchName: String? = null,
  val initiatorName: String? = null,
  val members: List<Member> = emptyList(),
  val approvers: List<Approver> = emptyList(),
  val currentUserId: UserId? = null,
  val commits: List<Commit> = emptyList(),
  val unreadCount: Int = 0,
  val mergeRequest: Branch.MergeRequest? = null,
  val mergeRequestVisible: Boolean = false,
  val mergeRequestInProgress: Boolean = false
) {
  val isCurrentUserApproved: Boolean
    get() = mergeRequest != null &&
      currentUserId != null &&
      currentUserId in mergeRequest.approvedByIds

  val cardShown: Boolean
    get() = mergeRequestVisible && mergeRequest != null
}
