package ru.kode.demo.core.routing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ru.kode.demo.core.ui.FlowEventSink
import ru.kode.way.Event

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
