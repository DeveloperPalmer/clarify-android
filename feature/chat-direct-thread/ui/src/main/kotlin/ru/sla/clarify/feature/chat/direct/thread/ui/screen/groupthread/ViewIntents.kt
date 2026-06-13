package ru.sla.clarify.feature.chat.direct.thread.ui.screen.groupthread

import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val sendMessage = intent<String>(name = "sendMessage")
  val openGroupInfo = intent(name = "openGroupInfo")
}
