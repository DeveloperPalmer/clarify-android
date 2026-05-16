package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation

@Immutable
data class ViewState(
  val contentLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val userId: String? = null,
  val conversations: List<Conversation> = emptyList(),
  val editModeEnabled: Boolean = false,
  val selectedConversationIds: List<Conversation.Id> = emptyList()
)
