package ru.sla.clarify.feature.chat.branch.ui.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data object BranchDismissed : FlowEvent
}
