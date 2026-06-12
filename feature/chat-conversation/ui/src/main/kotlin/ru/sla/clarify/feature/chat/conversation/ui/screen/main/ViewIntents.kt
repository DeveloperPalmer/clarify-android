package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.uikit.component.tabsrow.Tab
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val openProfile = intent(name = "openProfile")
  val openCreateConversation = intent(name = "showCreateConversation")
  val changeCreateConversationTab = intent<Tab>(name = "changeCreateConversationTab")

  val openDirectConversation = intent<Peer.Id>(name = "openDirectConversation")
  val openGroupConversation = intent<Conversation.Id>(name = "openGroupChat")

  val confirmCreateDirect = intent<String>(name = "confirmCreateDirect")
  val confirmCreateGroup = intent<String>(name = "confirmCreateConversationGroup")

  val handleConversationLongPress = intent<Conversation.Id>(name = "handleConversationLongPress")
  val openDeleteConversation = intent(name = "showDeleteConfirmation")
  val confirmDeleteConversation = intent(name = "confirmDeleteConversation")
}
