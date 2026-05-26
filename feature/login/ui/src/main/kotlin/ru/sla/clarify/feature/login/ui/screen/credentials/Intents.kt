package ru.sla.clarify.feature.login.ui.screen.credentials

import ru.kode.amvi.viewmodel.ViewIntents

class Intents : ViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val signInByEmail = intent(name = "signInByEmail")
  val signInByGoogle = intent(name = "signInByGoogle")
}
