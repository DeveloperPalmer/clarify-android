package ru.sla.clarify.feature.main.ui.screen.main

import ru.sla.clarify.uikit.scaffold.DialogDismissReason
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val dismissDialogError = intent<DialogDismissReason>(name = "handleGoogleIdTokenReceived")
  val dismissSnackbarError = intent(name = "dismissSnackbarError")
  val logout = intent(name = "logout")
}
