package ru.sla.clarify.feature.chat.branch.ui.screen

import ru.sla.clarify.feature.chat.branch.ui.screen.ViewState.DeleteCommitsParams
import ru.sla.clarify.uikit.component.chat.Commit
import java.time.LocalDateTime
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")

  val toggleSelectionMode = intent<Commit>(name = "toggleSelectionMode")
  val disableSelectionMode = intent(name = "disableSelectionMode")

  val sendMessage = intent<String>(name = "sendMessage")
  val copyMessage = intent(name = "copyMessage")
  val markMessageAsRead = intent<LocalDateTime>(name = "markMessageAsRead")

  val deleteCommit = intent<Commit>(name = "deleteCommit")
  val deleteCommits = intent(name = "deleteCommits")
  val confirmDeleteCommits = intent<DeleteCommitsParams>(name = "confirmDeleteCommits")

  val showMessageMenu = intent<Commit>(name = "showMessageMenu")
  val hideMessageMenu = intent(name = "hideMessageMenu")

  val openMergeRequest = intent(name = "openMergeRequest")
  val approveMergeRequest = intent(name = "approveMergeRequest")
  val revokeApprovalMergeRequest = intent(name = "revokeApprovalMergeRequest")
  val cancelMergeRequest = intent(name = "cancelMergeRequest")
  val finalizeMergeRequest = intent(name = "finalizeMergeRequest")
  val hideMergeRequest = intent(name = "hideMergeRequest")
}
