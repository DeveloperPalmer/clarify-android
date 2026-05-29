package ru.sla.clarify.feature.debug.panel.ui.screen.main

import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.debug.panel.ui.routing.FlowEvent
import javax.inject.Inject

class DebugPanelViewModel @Inject constructor(
  private val eventSink: FlowEventSink
) : ViewModel<ViewState, ViewIntents>() {
  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to null

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.DebugPanelDismissed)
      }
    }
  }
}
