package ru.sla.clarify.feature.chronology.ui.screen.chronology

import ru.sla.clarify.uikit.scaffold.DialogDismissReason
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val dismissDialogError = intent<DialogDismissReason>(name = "dismissDialogError")
  val dismissSnackbarError = intent(name = "dismissSnackbarError")
}
