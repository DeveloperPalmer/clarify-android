package ru.sla.clarify.feature.main.ui.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data object LogoutSuccessfully : FlowEvent
  data object OpenChats : FlowEvent
}
