package ru.sla.clarify.feature.chat.conversation.ui.routing

import ru.kode.way.Event
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer

sealed interface FlowEvent : Event {
  data object ChatListDismissed : FlowEvent
  data object ProfileRequested : FlowEvent
  data class DirectConversationRequested(val id: Peer.Id) : FlowEvent
  data class GroupConversationRequested(val id: Conversation.Id) : FlowEvent
}
