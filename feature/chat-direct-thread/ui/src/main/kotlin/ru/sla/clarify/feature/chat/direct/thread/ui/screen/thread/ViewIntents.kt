package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.CreateBranchError
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ViewState.CreateBranchParams
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ViewState.DeleteCommitsParams
import ru.sla.clarify.uikit.component.chat.Commit
import java.time.LocalDateTime
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents
import ru.sla.clarify.entity.chat.Commit as DomainCommit

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")

  val loadCommitsHistory = intent(name = "loadCommitsHistory")

  val toggleSelectionMode = intent<Commit>(name = "toggleSelectionMode")
  val disableSelectionMode = intent(name = "disableSelectionMode")

  val sendMessage = intent<String>(name = "sendMessage")
  val replyMessage = intent<String>(name = "replyMessage")
  val copyMessage = intent(name = "copyMessage")
  val markMessageAsRead = intent<LocalDateTime>(name = "markMessageAsRead")

  val deleteCommit = intent<Commit>(name = "deleteCommit")
  val deleteCommits = intent(name = "deleteCommits")
  val confirmDeleteCommits = intent<DeleteCommitsParams>(name = "confirmDeleteCommits")

  val showMessageMenu = intent<Commit>(name = "showMessageMenu")
  val hideMessageMenu = intent(name = "hideMessageMenu")

  val showReplyMessage = intent<Commit>(name = "showReplyMessage")
  val hideReplyMessage = intent(name = "hideReplyMessage")
  val showQuotedMessage = intent<DomainCommit.Id>(name = "showQuotedMessage")
  val clearHighlightedCommit = intent(name = "clearHighlightedCommit")

  val showEditMessage = intent<Commit>(name = "showEditMessage")
  val hideEditMessage = intent(name = "hideEditMessage")
  val confirmEditMessage = intent<String>(name = "confirmEditMessage")

  val createBranch = intent<Commit>(name = "createBranch")
  val confirmCreateBranch = intent<CreateBranchParams>(name = "confirmCreateBranch")
  val openBranch = intent<Branch.Id>(name = "openBranch")
  val showBranches = intent(name = "showBranches")
  val showCreateBranchError = intent<CreateBranchError>(name = "showCreateBranchError")
  val clearCreateBranchError = intent(name = "clearCreateBranchError")

  val openChronology = intent(name = "openChronology")
}
