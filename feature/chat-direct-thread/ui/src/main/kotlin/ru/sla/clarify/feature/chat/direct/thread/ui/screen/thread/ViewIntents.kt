package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.CreateBranchError
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ViewState.CreateBranchParams
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ViewState.DeleteCommitsParams
import ru.sla.clarify.uikit.component.chat.Commit
import java.time.LocalDateTime
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val sendMessage = intent<String>(name = "sendMessage")
  val markReadUpTo = intent<LocalDateTime>(name = "markReadUpTo")
  val loadCommitsHistory = intent(name = "loadCommitsHistory")

  val disableEditMode = intent(name = "disableEditMode")
  val toggleMessageSelection = intent<Commit>(name = "toggleMessageSelection")

  val deleteCommit = intent<Commit>(name = "deleteCommit")
  val deleteCommits = intent(name = "deleteCommits")
  val confirmDeleteCommit = intent<DeleteCommitsParams>(name = "confirmDeleteCommit")

  val showMessageMenu = intent<Commit.Message>(name = "showMessageMenu")
  val hideMessageMenu = intent(name = "hideMessageMenu")
  val copyMessage = intent(name = "copyMessage")

  val startEditMessage = intent<Commit.Message>(name = "startEditMessage")
  val cancelEditMessage = intent(name = "cancelEditMessage")
  val submitEditMessage = intent<String>(name = "submitEditMessage")

  val createBranch = intent<Commit.Message>(name = "createBranch")
  val confirmCreateBranchParams = intent<CreateBranchParams>(name = "confirmCreateBranch")
  val openBranch = intent<Branch.Id>(name = "openBranch")
  val showBranches = intent(name = "showBranches")
  val showCreateBranchError = intent<CreateBranchError>(name = "showCreateBranchError")
  val clearCreateBranchError = intent(name = "clearCreateBranchError")
}
