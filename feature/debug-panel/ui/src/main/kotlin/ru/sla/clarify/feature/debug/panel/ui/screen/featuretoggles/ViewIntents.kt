package ru.sla.clarify.feature.debug.panel.ui.screen.featuretoggles

import ru.sla.clarify.core.domain.toggle.AppFeature
import ru.kode.amvi.viewmodel.ViewIntents as BaseViewIntents

class ViewIntents : BaseViewIntents() {
  val navigateBack = intent(name = "navigateBack")
  val changeFeatureToggle = intent<Pair<AppFeature, Boolean>>(name = "changeFeatureToggle")
}
