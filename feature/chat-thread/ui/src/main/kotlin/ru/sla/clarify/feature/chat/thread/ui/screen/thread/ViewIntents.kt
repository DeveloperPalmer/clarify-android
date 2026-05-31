package ru.sla.clarify.feature.chat.thread.ui.screen.thread

import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.entity.Commit
import ru.sla.clarify.feature.chat.thread.ui.screen.thread.ViewState.CreateBranchPayload
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val sendMessage = intent<String>(name = "sendMessage")
  val showBranches = intent<Commit.Message>(name = "showBranches")
  val createBranch = intent<CreateBranchPayload>(name = "createBranch")
  val showBranchesList = intent(name = "showBranchesList")
  val openBranch = intent<Branch.Id>(name = "openBranch")
}
