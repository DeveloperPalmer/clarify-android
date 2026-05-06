package ru.sla.clarify.feature.chat.ui.routing

import ru.kode.way.Event

sealed interface ChatFlowEvent : Event {
  data class OpenChat(val peerUserId: String) : ChatFlowEvent
}
