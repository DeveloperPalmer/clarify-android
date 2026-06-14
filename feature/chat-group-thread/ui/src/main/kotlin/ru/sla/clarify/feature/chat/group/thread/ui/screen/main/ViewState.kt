package ru.sla.clarify.feature.chat.group.thread.ui.screen.main

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chat.group.thread.ui.entity.Commit
import ru.sla.clarify.feature.chat.group.thread.ui.entity.Group

@Immutable
data class ViewState(
  val group: Group? = null,
  val contentLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val commits: List<Commit> = emptyList(),
  val unreadCount: Int = 0
)
