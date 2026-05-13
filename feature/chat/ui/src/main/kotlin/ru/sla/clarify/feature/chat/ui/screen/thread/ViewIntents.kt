package ru.sla.clarify.feature.chat.ui.screen.thread

import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val openChronology = intent(name = "openChronology")
  val sendMessage = intent<String>(name = "sendMessage")
}
