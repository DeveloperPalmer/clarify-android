package ru.sla.clarify.feature.chat.ui.screen.list

import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val openChat = intent<String>(name = "openChat")
  val showNewChatDialog = intent(name = "showNewChatDialog")
  val dismissNewChatDialog = intent(name = "dismissNewChatDialog")
  val peerIdChanged = intent<String>(name = "peerIdChanged")
  val confirmNewChat = intent(name = "confirmNewChat")
  val navigateBack = intent(name = "navigateBack")
}
