package ru.sla.clarify.feature.chat.ui.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data object ChatThreadRequested : FlowEvent
  data object ChatThreadDismissed : FlowEvent
}
