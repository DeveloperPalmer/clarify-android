package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit

@Immutable
data class ViewState(
  val branchId: Branch.Id,
  val contentLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val branchName: String = "",
  val branchStatus: Branch.Status = Branch.Status.Active,
  val commits: List<Commit> = emptyList(),
  val isSending: Boolean = false
)
