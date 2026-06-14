package ru.sla.clarify.feature.chat.group.thread.ui.screen.main

import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val sendMessage = intent<String>(name = "sendMessage")
  val openGroupInfo = intent(name = "openGroupInfo")
}
