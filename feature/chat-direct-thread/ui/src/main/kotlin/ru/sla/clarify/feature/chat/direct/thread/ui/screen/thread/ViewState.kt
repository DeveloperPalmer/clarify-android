package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.CreateBranchError
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.entity.chat.Commit as DomainCommit

@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val peer: Peer? = null,
  val commits: List<Commit> = emptyList(),
  val canLoadCommitsHistory: Boolean = true,
  val loadingCommitsHistory: Boolean = false,
  val branches: List<Branch> = emptyList(),
  val createBranchError: CreateBranchError? = null,
  val unreadCount: Int = 0,
  val selectionEnabled: Boolean = false,
  val selectedCommitIds: List<DomainCommit.Id> = emptyList(),
  val focusedCommit: Commit? = null,
  val editingCommit: Commit? = null,
  val replyingCommit: Commit? = null,
  val highlightedCommitId: DomainCommit.Id? = null
) {

  @Immutable
  data class CreateBranchParams(
    val name: String,
    val commit: Commit
  )

  @Immutable
  data class DeleteCommitsParams(
    val ids: List<DomainCommit.Id>,
    val forEveryone: Boolean
  )
}
