package ru.sla.clarify.feature.debug.panel.ui.screen.featuretoggles

import androidx.compose.runtime.Immutable
import ru.sla.clarify.core.domain.toggle.AppFeature

@Immutable
data class ViewState(
  val featureToggles: List<FeatureToggle> = emptyList()
)

@Immutable
data class FeatureToggle(
  val feature: AppFeature,
  val isEnabled: Boolean,
  val description: String
)
