package ru.sla.clarify.feature.chat.ui.screen.list

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.entity.UiError
import ru.sla.clarify.feature.chat.domain.entity.Conversation

@Immutable
data class ViewState(
  val myUserId: String? = null,
  val conversations: List<Conversation> = emptyList(),
  val newChatDialogVisible: Boolean = false,
  val peerIdInput: String = "",
  val contentLoadState: ContentLoadState = ContentLoadState.Loading,
  val dialogError: UiError? = null,
  val snackbarError: UiError? = null
)
