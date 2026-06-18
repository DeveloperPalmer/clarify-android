package ru.sla.clarify.feature.chat.group.thread.ui.screen.main

import java.time.LocalDateTime
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val sendMessage = intent<String>(name = "sendMessage")
  val markReadUpTo = intent<LocalDateTime>(name = "markReadUpTo")
  val openGroupInfo = intent(name = "openGroupInfo")
}
