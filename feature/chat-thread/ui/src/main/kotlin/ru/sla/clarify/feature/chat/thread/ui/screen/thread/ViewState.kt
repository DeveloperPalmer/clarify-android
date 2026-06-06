package ru.sla.clarify.feature.chat.thread.ui.screen.thread

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.entity.Commit
import ru.sla.clarify.feature.chat.thread.ui.entity.CreateBranchError
import ru.sla.clarify.feature.entity.chat.Peer

@Immutable
data class ViewState(
  val peer: Peer? = null,
  val contentLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val commits: List<Commit> = emptyList(),
  val branches: List<Branch> = emptyList(),
  val unreadCount: Int = 0,
  val createBranchError: CreateBranchError? = null
) {
  data class CreateBranchPayload(
    val commit: Commit.Message,
    val name: String
  )
}
