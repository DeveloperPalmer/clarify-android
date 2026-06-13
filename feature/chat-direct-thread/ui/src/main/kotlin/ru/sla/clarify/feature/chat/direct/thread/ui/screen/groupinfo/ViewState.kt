package ru.sla.clarify.feature.chat.direct.thread.ui.screen.groupinfo

import androidx.compose.runtime.Immutable
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.Group
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.GroupMember
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.InviteCandidate

@Immutable
data class ViewState(
  val group: Group? = null,
  val members: List<GroupMember> = emptyList(),
  val isOwner: Boolean = false,
  val searchQuery: String = "",
  val searchResults: List<InviteCandidate> = emptyList(),
  val selectedCandidates: List<InviteCandidate> = emptyList(),
  val isInviteLimitReached: Boolean = false
)
