package ru.sla.clarify.feature.chat.direct.thread.ui.routing

import ru.kode.way.Event
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Peer

sealed interface FlowEvent : Event {
  data object DirectThreadDismissed : FlowEvent
  data class BranchRequested(val branchId: Branch.Id) : FlowEvent
  data class ChronologyRequested(val peerId: Peer.Id) : FlowEvent
}
