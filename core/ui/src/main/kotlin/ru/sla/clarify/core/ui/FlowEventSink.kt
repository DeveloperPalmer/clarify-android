package ru.sla.clarify.core.ui

import ru.kode.way.Event

interface FlowEventSink {
  fun sendEvent(e: Event)
}
