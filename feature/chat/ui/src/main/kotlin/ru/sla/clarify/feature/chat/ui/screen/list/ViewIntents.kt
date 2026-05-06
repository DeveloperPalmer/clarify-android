package ru.sla.clarify.feature.chat.ui.screen.list

import ru.sla.clarify.uikit.scaffold.DialogDismissReason
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val openChat = intent<String>(name = "openChat")
  val showNewChatDialog = intent(name = "showNewChatDialog")
  val dismissNewChatDialog = intent(name = "dismissNewChatDialog")
  val peerIdChanged = intent<String>(name = "peerIdChanged")
  val confirmNewChat = intent(name = "confirmNewChat")
  val navigateBack = intent(name = "navigateBack")
  val dismissSnackbarError = intent(name = "dismissSnackbarError")
  val dismissDialogError = intent<DialogDismissReason>(name = "dismissDialogError")
}
