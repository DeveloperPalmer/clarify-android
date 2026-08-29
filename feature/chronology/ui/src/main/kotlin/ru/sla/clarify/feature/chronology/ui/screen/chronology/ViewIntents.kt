package ru.sla.clarify.feature.chronology.ui.screen.chronology

import ru.sla.clarify.feature.chronology.ui.entity.GraphNodeSelection
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val toggleDebugOverlay = intent(name = "toggleDebugOverlay")

  val selectNode = intent<GraphNodeSelection>(name = "selectNode")
  val closeNodePreview = intent(name = "closeNodePreview")
}
