package ru.sla.clarify.feature.chat.ui.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data class ChatThreadRequested(val peerId: String) : FlowEvent
  data object ChatThreadDismissed : FlowEvent
  data object ChronologyRequested : FlowEvent
}
