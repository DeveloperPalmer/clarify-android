package ru.sla.clarify.feature.chat.conversation.ui.routing

import ru.kode.way.Event
import ru.sla.clarify.feature.entity.chat.Peer

sealed interface FlowEvent : Event {
  data object ChatListDismissed : FlowEvent
  data class ThreadRequested(val id: Peer.Id) : FlowEvent
}
