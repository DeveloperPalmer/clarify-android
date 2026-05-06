package ru.sla.clarify.feature.chat.ui.screen.thread

import ru.sla.clarify.uikit.scaffold.DialogDismissReason
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val inputChanged = intent<String>(name = "inputChanged")
  val sendMessage = intent(name = "sendMessage")
  val navigateBack = intent(name = "navigateBack")
  val dismissSnackbarError = intent(name = "dismissSnackbarError")
  val dismissDialogError = intent<DialogDismissReason>(name = "dismissDialogError")
}
