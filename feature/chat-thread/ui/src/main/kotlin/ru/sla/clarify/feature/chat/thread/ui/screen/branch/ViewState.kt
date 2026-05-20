package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState

@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.Ready
)
