package ru.sla.clarify.feature.chat.group.thread.ui.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data object GroupThreadDismissed : FlowEvent
  data object GroupInfoRequested : FlowEvent
  data object GroupInfoDismissed : FlowEvent
  data object GroupClosed : FlowEvent
}
