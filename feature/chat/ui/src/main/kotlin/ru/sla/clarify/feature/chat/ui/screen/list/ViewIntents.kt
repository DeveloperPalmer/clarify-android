package ru.sla.clarify.feature.chat.ui.screen.list

import ru.sla.clarify.feature.chat.domain.entity.Conversation
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val openChat = intent<String>(name = "openChat")
  val showNewChatDialog = intent(name = "showNewChatDialog")
  val dismissNewChatDialog = intent(name = "dismissNewChatDialog")
  val confirmNewChat = intent<String>(name = "confirmNewChat")
  val handleConversationLongPress = intent<Conversation.Id>(name = "handleConversationLongPress")
  val showDeleteConfirmation = intent(name = "showDeleteConfirmation")
  val confirmDeleteConversation = intent(name = "confirmDeleteConversation")
  val openSettings = intent(name = "openSettings")
}
