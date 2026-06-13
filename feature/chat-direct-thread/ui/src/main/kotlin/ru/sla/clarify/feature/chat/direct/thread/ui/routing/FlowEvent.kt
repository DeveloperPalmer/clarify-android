package ru.sla.clarify.feature.chat.direct.thread.ui.routing

import ru.kode.way.Event
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.Branch

sealed interface FlowEvent : Event {
  data object ThreadDismissed : FlowEvent
  data class BranchRequested(val branchId: Branch.Id) : FlowEvent
  data object BranchDismissed : FlowEvent
  data object GroupInfoRequested : FlowEvent
  data object GroupInfoDismissed : FlowEvent
  data object GroupClosed : FlowEvent
}
