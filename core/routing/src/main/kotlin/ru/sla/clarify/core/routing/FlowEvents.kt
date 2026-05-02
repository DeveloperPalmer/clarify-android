package ru.sla.clarify.core.routing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ru.kode.way.Event
import ru.sla.clarify.core.ui.FlowEventSink

interface FlowEventSource {
  val events: Flow<Event>
}

class FlowEventMediator(
  private val scope: CoroutineScope
) : FlowEventSink, FlowEventSource {
  private val _events = MutableSharedFlow<Event>()

  override fun sendEvent(e: Event) {
    scope.launch {
      _events.emit(e)
    }
  }

  override val events: Flow<Event> = _events
}
