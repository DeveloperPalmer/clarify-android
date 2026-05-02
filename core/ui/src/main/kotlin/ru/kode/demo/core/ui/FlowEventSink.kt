package ru.kode.demo.core.ui

import ru.kode.way.Event

interface FlowEventSink {
  fun sendEvent(e: Event)
}
