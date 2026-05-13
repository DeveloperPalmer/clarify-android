package ru.sla.clarify.feature.main.ui.screen.main

import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.ui.FlowEventSink
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
        // nothing to do
      }
    }

    onEach(intent(ViewIntents::openChats)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.OpenChats)
      }
    }

    configureLoginTransitions()
    configureLogoutTransitions()
  }

  private fun MachineDsl<ViewState>.configureLoginTransitions() {
    onEach(
      mainModel.chatSignIn.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }
  }

  private fun MachineDsl<ViewState>.configureLogoutTransitions() {
    onEach(
      mainModel.signOut.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }

    onEach(mainModel.signOut.jobFlow.successResults()) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.LogoutSuccessfully)
      }
    }

    onEach(intent(ViewIntents::logout)) {
      action { _, _, _ ->
        mainModel.signOut.start()
      }
    }
  }
}
