package ru.sla.clarify.feature.login.ui.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data object GoogleSignInSucceeded : FlowEvent
}
