package ru.sla.clarify.feature.login.ui.screen.credentials

import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.JobState
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.kode.way.Back
import ru.kode.way.Event
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.feature.login.domain.LoginModel
import ru.sla.clarify.feature.login.ui.routing.FlowEvent
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.strRef
import javax.inject.Inject

class CredentialsViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val loginModel: LoginModel
) : ViewModel<ViewState, Intents>() {
  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to null

    onEach(intent(Intents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(Event.Back)
      }
    }

    onEach(intent(Intents::signIn)) {
      action { _, _, _ ->
        loginModel.signIn.start()
      }
    }

    onEach(loginModel.signIn.jobFlow.state) {
      transitionTo { state, jobState ->
        state.copy(processing = jobState == JobState.Running)
      }
    }

    onEach(loginModel.signIn.jobFlow.successResults()) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.GoogleSignInSucceeded)
      }
    }

    onEach(loginModel.signIn.jobFlow.errors()) {
      action { _, _, error ->
        val message = error.message
        if (message != null) {
          sendViewEvent(Snackbar(message = strRef(message), isError = true))
        }
      }
    }
  }
}
