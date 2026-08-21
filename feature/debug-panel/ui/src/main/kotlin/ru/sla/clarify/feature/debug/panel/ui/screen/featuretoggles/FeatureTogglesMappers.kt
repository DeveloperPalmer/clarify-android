package ru.sla.clarify.feature.debug.panel.ui.screen.featuretoggles

import ru.sla.clarify.core.domain.toggle.AppFeature

internal fun extractFeatureToggle(feature: AppFeature, isEnabled: Boolean): FeatureToggle {
  return when (feature) {
    AppFeature.GroupsAvailable -> FeatureToggle(
      feature = feature,
      isEnabled = isEnabled,
      description = "Enable group chats: group tab in the create dialog and group conversations in the chat list"
    )

    AppFeature.ChronologyDebugOverlay -> FeatureToggle(
      feature = feature,
      isEnabled = isEnabled,
      description = "Show the chronology graph camera readout: viewport, content bounds, pan limits and drag events"
    )
  }
}
