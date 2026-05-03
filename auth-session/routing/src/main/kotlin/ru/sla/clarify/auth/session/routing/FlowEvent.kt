package ru.sla.clarify.auth.session.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data object LogoutRequested : FlowEvent
}
