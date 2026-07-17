package ru.sla.clarify.feature.login.ui.screen.credentials

import me.tatarka.inject.annotations.Inject
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.JobState
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.kode.way.Back
import ru.kode.way.Event
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.feature.login.domain.LoginModel
import ru.sla.clarify.feature.login.entity.GoogleAuthError
import ru.sla.clarify.feature.login.ui.routing.FlowEvent
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef
import ru.sla.resourcerefs.strRef

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

    onEach(intent(Intents::signInByEmail)) {
      action { _, _, _ ->
        sendViewEvent(Snackbar(resRef(R.string.not_yet_implemented_message)))
      }
    }

    onEach(intent(Intents::signInByGoogle)) {
      action { _, _, _ ->
        loginModel.signInByGoogle.start()
      }
    }

    onEach(loginModel.signInByGoogle.jobFlow.state) {
      transitionTo { state, jobState ->
        state.copy(googleInProgress = jobState == JobState.Running)
      }
    }

    onEach(loginModel.signInByGoogle.jobFlow.successResults()) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.GoogleSignInSucceeded)
      }
    }

    onEach(loginModel.signInByGoogle.jobFlow.errors()) {
      action { _, _, error ->
        if (error.cause is GoogleAuthError.CancelledByUser) {
          return@action
        }
        val message = error.message
        if (message != null) {
          sendViewEvent(Snackbar(message = strRef(message), isError = true))
        }
      }
    }
  }
}
