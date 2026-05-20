package ru.sla.clarify.feature.chat.thread.ui.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data object ThreadDismissed : FlowEvent
  data object BranchRequested : FlowEvent
  data object BranchDismissed : FlowEvent
}
