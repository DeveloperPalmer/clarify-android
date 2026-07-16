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
  val peer: Peer? = null,
  val contentLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val commits: List<Commit> = emptyList(),
  val branches: List<Branch> = emptyList(),
  val unreadCount: Int = 0,
  val createBranchError: CreateBranchError? = null,
  val editModeEnabled: Boolean = false,
  val selectedCommitIds: List<DomainCommit.Id> = emptyList(),
  val focusedMessage: Commit.Message? = null
) {

  @Immutable
  data class CreateBranchParams(
    val name: String,
    val commit: Commit.Message
  )

  @Immutable
  data class DeleteCommitsParams(
    val ids: List<DomainCommit.Id>,
    val forEveryone: Boolean
  )
}
