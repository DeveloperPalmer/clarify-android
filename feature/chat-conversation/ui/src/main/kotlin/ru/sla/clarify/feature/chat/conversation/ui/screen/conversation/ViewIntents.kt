package ru.sla.clarify.feature.chat.conversation.ui.screen.conversation

import androidx.compose.ui.text.input.TextFieldValue
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.uikit.component.tabsrow.Tab
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val openProfile = intent(name = "openProfile")
  val openDirectConversation = intent<Peer.Id>(name = "openDirectConversation")

  val openCreateConversation = intent(name = "openCreateConversation")
  val hideCreateConversation = intent(name = "hideCreateConversation")
  val changeCreateConversationTab = intent<Tab>(name = "changeCreateConversationTab")

  val changeEmailQuery = intent<TextFieldValue>(name = "changeEmailQuery")
  val changeGroupNameQuery = intent<TextFieldValue>(name = "changeGroupNameQuery")

  val validateGroupName = intent(name = "validateGroupName")
  val validateDirectEmail = intent(name = "validateDirectEmail")

  val showCreateDirectConversationError = intent<Email.Error>(name = "showCreateDirectConversationError")
  val hideCreateDirectConversationError = intent(name = "hideDirectConversationError")

  val openGroupConversation = intent<Conversation.Id>(name = "openGroupConversation")

  val confirmCreateGroup = intent(name = "confirmCreateGroup")
  val confirmCreateDirectConversation = intent(name = "confirmDirectConversation")

  val handleConversationLongPress = intent<Conversation.Id>(name = "handleConversationLongPress")
  val openDeleteConversation = intent(name = "openDeleteConversation")
  val confirmDeleteConversation = intent(name = "confirmDeleteConversation")
}
