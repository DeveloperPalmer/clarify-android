package ru.kode.demo.feature.main.ui.screen.main

import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.demo.core.ui.FlowEventSink
import ru.kode.way.Back
import ru.kode.way.Event
import javax.inject.Inject

class MainViewModel @Inject constructor(
  private val eventSink: FlowEventSink
) : ViewModel<ViewState, ViewIntents>() {
  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState to null
    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(Event.Back)
      }
    }
  }
}
