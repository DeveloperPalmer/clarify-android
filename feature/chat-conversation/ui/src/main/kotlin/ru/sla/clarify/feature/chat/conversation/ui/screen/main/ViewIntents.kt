package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val openChat = intent<Peer.Id>(name = "openChat")
  val showNewChatDialog = intent(name = "showNewChatDialog")
  val confirmNewChat = intent<Peer.Id>(name = "confirmNewChat")
  val handleConversationLongPress = intent<Conversation.Id>(name = "handleConversationLongPress")
  val showDeleteConfirmation = intent(name = "showDeleteConfirmation")
  val confirmDeleteConversation = intent(name = "confirmDeleteConversation")
  val openSettings = intent(name = "openSettings")
}
