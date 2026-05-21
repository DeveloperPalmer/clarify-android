package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val sendCommit = intent<String>(name = "sendCommit")
  val requestMerge = intent(name = "requestMerge")
}
