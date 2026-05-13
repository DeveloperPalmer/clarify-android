package ru.sla.clarify.feature.chat.ui.screen.list

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chat.domain.entity.Conversation

@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val myUserId: String? = null,
  val conversations: List<Conversation> = emptyList(),

  val newChatDialogVisible: Boolean = false,

  val peerIdInput: String = "",
  val conversationDeletionId: Conversation.Id? = null
)
