package ru.sla.clarify.feature.chat.ui.screen.thread

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.entity.UiError
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage

@Immutable
data class ViewState(
  val peerUserId: String = "",
  val messages: List<ChatMessage> = emptyList(),
  val inputValue: String = "",
  val isSending: Boolean = false,
  val contentLoadState: ContentLoadState = ContentLoadState.Loading,
  val dialogError: UiError? = null,
  val snackbarError: UiError? = null
)
