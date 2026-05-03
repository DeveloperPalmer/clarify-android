package ru.sla.clarify.feature.login.ui.screen.splashintro

import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.way.Back
import ru.kode.way.Event
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.login.ui.routing.FlowEvent
import javax.inject.Inject

class SplashIntroViewModel @Inject constructor(
  private val eventSink: FlowEventSink
) : ViewModel<ViewState, Intents>() {
  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState to null

    onEach(intent(Intents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(Event.Back)
      }
    }

    onEach(intent(Intents::signIn)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.RegistrationRequested)
      }
    }
  }
}
