package ru.sla.clarify.feature.login.ui.screen.splashintro

import ru.kode.amvi.viewmodel.ViewIntents
import ru.sla.clarify.uikit.scaffold.DialogDismissReason

class Intents : ViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val dismissSnackbarError = intent(name = "dismissSnackbarError")
  val dismissDialogError = intent<DialogDismissReason>(name = "handleGoogleIdTokenReceived")

  val signIn = intent(name = "signIn")
}
