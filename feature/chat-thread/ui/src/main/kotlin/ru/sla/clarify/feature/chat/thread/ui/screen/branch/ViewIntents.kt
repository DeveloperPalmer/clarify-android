package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val sendCommit = intent<String>(name = "sendCommit")
  val requestMerge = intent(name = "requestMerge")

  /** Со стороны peer'а: подтвердить чужой merge request. */
  val approveMerge = intent(name = "approveMerge")

  /**
   * Универсальный «откатить approval»:
   * - peer, который уже approve'нул -> убирает своё одобрение, merge request продолжает жить;
   * - инициатор -> отменяет весь merge request, ветка возвращается в Active.
   * Data-слой разруливает один из двух кейсов, читая документ внутри транзакции.
   */
  val revokeApproval = intent(name = "revokeApproval")
}
