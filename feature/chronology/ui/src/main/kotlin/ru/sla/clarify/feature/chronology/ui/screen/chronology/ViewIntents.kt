package ru.sla.clarify.feature.chronology.ui.screen.chronology

import ru.sla.atlas.entity.Node
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val toggleDebugOverlay = intent(name = "toggleDebugOverlay")

  val selectNode = intent<Node.Id>(name = "selectNode")
  val closeNodePreview = intent(name = "closeNodePreview")
}
