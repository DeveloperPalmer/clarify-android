package ru.sla.clarify.feature.chat.ui.screen.thread

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage

@Immutable
data class ViewState(
  val peerId: String,
  val messages: List<ChatMessage> = emptyList(),
  val isSending: Boolean = false,
  val contentLoadState: ContentLoadState = ContentLoadState.Loading
)
