package ru.sla.clarify.feature.profile.ui.routing

import ru.kode.way.Event

sealed interface FlowEvent : Event {
  data object LogoutSuccessfully : FlowEvent
  data object ProfileDismissed : FlowEvent
}
