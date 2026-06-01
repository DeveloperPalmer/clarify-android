package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import java.time.LocalDateTime
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val sendCommit = intent<String>(name = "sendCommit")
  val markReadUpTo = intent<LocalDateTime>(name = "markReadUpTo")
  val openMergeRequest = intent(name = "openMergeRequest")
  val approveMergeRequest = intent(name = "approveMergeRequest")
  val revokeApprovalMergeRequest = intent(name = "revokeApprovalMergeRequest")
  val cancelMergeRequest = intent(name = "cancelMergeRequest")
  val finalizeMergeRequest = intent(name = "finalizeMergeRequest")
}
