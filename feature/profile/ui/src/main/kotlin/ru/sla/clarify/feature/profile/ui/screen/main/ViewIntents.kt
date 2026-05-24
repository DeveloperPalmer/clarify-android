package ru.sla.clarify.feature.profile.ui.screen.main

import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val logout = intent(name = "logout")
}
