package ru.sla.clarify.feature.chat.branch.ui.screen

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.feature.chat.branch.ui.entity.Approver
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.entity.chat.Commit as DomainCommit

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
  val mergeRequestInProgress: Boolean = false,
  val editModeEnabled: Boolean = false,
  val selectedCommitIds: List<DomainCommit.Id> = emptyList(),
  val focusedMessage: Commit.Message? = null
) {
  val isCurrentUserApproved: Boolean
    get() = mergeRequest != null &&
      currentUserId != null &&
      currentUserId in mergeRequest.approvedByIds

  val cardShown: Boolean
    get() = mergeRequestVisible && mergeRequest != null

  val peerName: String?
    get() = members
      .firstOrNull { it.id.value != currentUserId?.value }
      ?.displayName

  @Immutable
  data class DeleteCommitsParams(
    val ids: List<DomainCommit.Id>,
    val forEveryone: Boolean
  )
}
