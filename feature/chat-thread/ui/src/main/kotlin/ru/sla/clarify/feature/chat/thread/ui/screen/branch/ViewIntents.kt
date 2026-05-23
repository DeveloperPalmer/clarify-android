package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val sendCommit = intent<String>(name = "sendCommit")
  val requestMerge = intent(name = "requestMerge")

  /** Peer-side: confirm someone else's merge request. */
  val approveMerge = intent(name = "approveMerge")

  /**
   * Universal "step back from approving":
   * - peer who already approved -> removes own approval, merge request stays in flight;
   * - initiator -> cancels the entire merge request, branch returns to Active.
   * The data layer disambiguates by reading the document inside a transaction.
   */
  val revokeApproval = intent(name = "revokeApproval")
}
