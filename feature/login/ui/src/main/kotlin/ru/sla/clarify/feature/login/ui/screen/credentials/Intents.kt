package ru.sla.clarify.feature.login.ui.screen.credentials

import ru.kode.amvi.viewmodel.ViewIntents

class Intents : ViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val signIn = intent(name = "signIn")
}
