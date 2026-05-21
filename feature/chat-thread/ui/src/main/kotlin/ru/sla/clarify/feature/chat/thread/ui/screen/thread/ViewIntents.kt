package ru.sla.clarify.feature.chat.thread.ui.screen.thread

import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.screen.thread.ViewState.CreateBranchPayload
import ru.sla.clarify.feature.entity.chat.Commit
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val sendCommit = intent<String>(name = "sendCommit")
  val showBranchSheet = intent<Commit.Message>(name = "showBranchSheet")
  val createBranch = intent<CreateBranchPayload>(name = "createBranch")
  val showBranchesList = intent(name = "showBranchesList")
  val openBranch = intent<Branch.Id>(name = "openBranch")
}
