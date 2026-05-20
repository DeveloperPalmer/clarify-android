package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.feature.chat.thread.ui.routing.FlowEvent
import javax.inject.Inject

class BranchViewModel @Inject constructor(
  private val eventSink: FlowEventSink
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to null

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.BranchDismissed)
      }
    }
  }
}
