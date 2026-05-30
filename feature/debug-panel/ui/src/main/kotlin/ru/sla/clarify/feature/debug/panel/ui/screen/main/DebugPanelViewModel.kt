package ru.sla.clarify.feature.debug.panel.ui.screen.main

import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.debug.panel.domain.DebugPanelModel
import ru.sla.clarify.feature.debug.panel.domain.entity.DebugUserException
import ru.sla.clarify.feature.debug.panel.ui.routing.FlowEvent
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef
import javax.inject.Inject

class DebugPanelViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val debugPanelModel: DebugPanelModel
) : ViewModel<ViewState, ViewIntents>() {
  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to null

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.DebugPanelDismissed)
      }
    }

    onEach(intent(ViewIntents::changeUserField)) {
      transitionTo { state, json ->
        state.copy(userField = json, userJsonError = null)
      }
    }

    onEach(intent(ViewIntents::createUser)) {
      action { state, _, _ ->
        debugPanelModel.createUser.start(state.userField)
      }
    }

    onEach(
      debugPanelModel.createUser.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, jobState ->
        state.copy(userSending = jobState == ContentLoadState.Loading)
      }
    }

    onEach(debugPanelModel.createUser.jobFlow.successResults()) {
      transitionTo { state, _ ->
        state.copy(userField = "", userJsonError = null)
      }
      action { _, _, _ ->
        sendViewEvent(Snackbar(resRef(R.string.debug_panel_user_created)))
      }
    }

    onEach(debugPanelModel.createUser.jobFlow.errors()) {
      transitionTo { state, error ->
        if (error is DebugUserException) {
          state.copy(userJsonError = error.error)
        } else {
          state
        }
      }
      action { _, _, error ->
        if (error !is DebugUserException) {
          sendViewEvent(Snackbar(resRef(R.string.debug_panel_user_create_failed), isError = true))
        }
      }
    }
  }
}
