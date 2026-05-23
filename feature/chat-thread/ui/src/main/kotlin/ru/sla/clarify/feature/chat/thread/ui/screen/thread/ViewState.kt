package ru.sla.clarify.feature.chat.thread.ui.screen.thread

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit

@Immutable
data class ViewState(
  val email: Email? = null,
  val contentLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val commits: List<Commit> = emptyList(),
  val branches: List<Branch> = emptyList(),
  val isSending: Boolean = false
) {
  data class CreateBranchPayload(
    val commit: Commit.Message,
    val name: String
  )
}
