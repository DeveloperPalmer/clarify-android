package ru.sla.clarify.feature.chat.thread.ui.screen.thread

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Peer

@Immutable
data class ViewState(
  val peerId: Peer.Id,
  val commits: List<Commit> = emptyList(),
  val isSending: Boolean = false,
  val contentLoadState: ContentLoadState = ContentLoadState.Loading
)
