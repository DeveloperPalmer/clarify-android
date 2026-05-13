package ru.sla.clarify.feature.chat.ui.screen.list

import ru.sla.clarify.feature.chat.domain.entity.Conversation
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val openChat = intent<String>(name = "openChat")
  val showNewChatDialog = intent(name = "showNewChatDialog")
  val dismissNewChatDialog = intent(name = "dismissNewChatDialog")
  val peerIdChanged = intent<String>(name = "peerIdChanged")
  val confirmNewChat = intent(name = "confirmNewChat")
  val dismissDeleteMenu = intent(name = "dismissDeleteMenu")
  val showDeleteMenu = intent<Conversation.Id>(name = "showDeleteMenu")
  val showDeleteConfirmation = intent(name = "requestDeleteConfirmation")
  val confirmDeleteConversation = intent<Conversation.Id>(name = "confirmDeleteConversation")
}
