package ru.sla.clarify.feature.main.ui.screen.main

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.kode.way.Back
import ru.kode.way.Event
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.mapper.toAppUiError
import ru.sla.clarify.core.ui.mergedState
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.main.domain.MainModel
import ru.sla.clarify.feature.main.ui.routing.FlowEvent
import javax.inject.Inject

class MainViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val mainModel: MainModel
) : ViewModel<ViewState, ViewIntents>() {
  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to { mainModel.chatSignIn.start() }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(Event.Back)
      }
    }

    onEach(intent(ViewIntents::logout)) {
      action { _, _, _ ->
        mainModel.signOut.start()
      }
    }

    onEach(intent(ViewIntents::openChats)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.OpenChats)
      }
    }

    onEach(
      combine(
        mainModel.signOut.jobFlow
          .asLceState()
          .map { it.toUiLceState() },
        mainModel.chatSignIn.jobFlow
          .asLceState()
          .map { it.toUiLceState() }
      ) { signOutState, chatLoginState ->
        listOf(
          signOutState,
          chatLoginState
        ).mergedState()
      }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }

    onEach(mainModel.signOut.jobFlow.errors()) {
      transitionTo { state, error ->
        state.copy(snackbarError = error.toAppUiError())
      }
    }

    onEach(mainModel.signOut.jobFlow.successResults()) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.LogoutSuccessfully)
      }
    }

    onEach(mainModel.chatSignIn.jobFlow.errors()) {
      transitionTo { state, error ->
        state.copy(snackbarError = error.toAppUiError())
      }
    }
  }
}
