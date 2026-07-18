package ru.sla.clarify.feature.chat.branch.ui.screen

import ru.sla.clarify.feature.chat.branch.ui.screen.ViewState.DeleteCommitsParams
import ru.sla.clarify.uikit.component.chat.Commit
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

  val hideMergeRequest = intent(name = "hideMergeRequest")

  val disableEditMode = intent(name = "disableEditMode")
  val toggleMessageSelection = intent<Commit>(name = "toggleMessageSelection")

  val deleteCommit = intent<Commit>(name = "deleteCommit")
  val deleteCommits = intent(name = "deleteCommits")
  val confirmDeleteCommit = intent<DeleteCommitsParams>(name = "confirmDeleteCommit")

  val showMessageMenu = intent<Commit.Message>(name = "showMessageMenu")
  val hideMessageMenu = intent(name = "hideMessageMenu")
  val copyMessage = intent(name = "copyMessage")
}
