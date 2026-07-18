package ru.sla.clarify.feature.debug.panel.ui.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data object DebugPanelDismissed : FlowEvent
  data object FeatureTogglesRequested : FlowEvent
  data object FeatureTogglesDismissed : FlowEvent
}
