package ru.sla.clarify.feature.chat.thread.ui.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data object ThreadDismissed : FlowEvent
}
