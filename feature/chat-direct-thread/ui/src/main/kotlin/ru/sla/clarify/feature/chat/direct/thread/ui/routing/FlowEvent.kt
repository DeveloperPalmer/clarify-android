package ru.sla.clarify.feature.chat.direct.thread.ui.routing

import ru.kode.way.Event
import ru.sla.clarify.entity.chat.Branch

sealed interface FlowEvent : Event {
  data object ThreadDismissed : FlowEvent
  data class BranchRequested(val branchId: Branch.Id) : FlowEvent
}
