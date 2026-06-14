package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.entity.chat.Conversation
import ru.sla.clarify.uikit.component.tabsrow.Tab
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.resRef

@Immutable
data class ViewState(
  val user: User? = null,
  val conversations: List<Conversation> = emptyList(),
  val editModeEnabled: Boolean = false,
  val selectedConversationsIds: List<Conversation.Id> = emptyList(),
  val selectedCreateConversationOption: Tab = CreateConversationOption.Direct
) {
  enum class CreateConversationOption(override val title: TextRef) : Tab {
    Direct(title = resRef(R.string.conversation_new_tab_direct)),
    Group(title = resRef(R.string.conversation_new_tab_group))
  }
}
