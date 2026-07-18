package ru.sla.clarify.feature.chat.conversation.ui.screen.conversation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.GroupName
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.uikit.component.tabsrow.Tab
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.resRef

@Immutable
data class ViewState(
  val user: User? = null,
  val groupsAvailable: Boolean = false,
  val conversations: List<Conversation> = emptyList(),
  val editModeEnabled: Boolean = false,
  val selectedConversationsIds: List<Conversation.Id> = emptyList(),
  val selectedCreateConversationTab: CreateConversationTab = CreateConversationTab.Direct,
  val createConversationLoadState: ContentLoadState = ContentLoadState.NotStarted,
  val createConversationVisible: Boolean = false,

  val directEmailQuery: TextFieldValue = TextFieldValue(),
  val directEmailError: Email.Error? = null,
  val groupNameQuery: TextFieldValue = TextFieldValue(),
  val groupNameError: GroupName.Error? = null
) {
  enum class CreateConversationTab(override val title: TextRef) : Tab {
    Direct(title = resRef(R.string.conversation_tab_direct)),
    Group(title = resRef(R.string.conversation_tab_group))
  }
}
