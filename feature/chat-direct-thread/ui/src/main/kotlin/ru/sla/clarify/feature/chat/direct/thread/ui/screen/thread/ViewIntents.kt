package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.CreateBranchError
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ViewState.CreateBranchPayload
import ru.sla.clarify.uikit.component.chat.Commit
import java.time.LocalDateTime
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val sendMessage = intent<String>(name = "sendMessage")
  val markReadUpTo = intent<LocalDateTime>(name = "markReadUpTo")
  val createBranch = intent<Commit.Message>(name = "createBranch")
  val confirmCreateBranch = intent<CreateBranchPayload>(name = "confirmCreateBranch")
  val showCreateBranchError = intent<CreateBranchError>(name = "showCreateBranchError")
  val clearCreateBranchError = intent(name = "clearCreateBranchError")
  val showBranchesList = intent(name = "showBranchesList")
  val openBranch = intent<Branch.Id>(name = "openBranch")
}
