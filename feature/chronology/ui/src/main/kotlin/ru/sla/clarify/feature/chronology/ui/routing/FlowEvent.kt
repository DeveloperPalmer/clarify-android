package ru.sla.clarify.feature.chronology.ui.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data object ChronologyDismissed : FlowEvent
}
