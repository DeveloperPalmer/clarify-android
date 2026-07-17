package ru.sla.clarify.feature.profile.ui.screen.main

import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.profile.domain.ProfileModel
import ru.sla.clarify.feature.profile.ui.routing.FlowEvent

class ProfileViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val profileModel: ProfileModel
) : ViewModel<ViewState, ViewIntents>() {
  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to null

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ProfileDismissed)
      }
    }

    configureLogoutTransitions()
  }

  private fun MachineDsl<ViewState>.configureLogoutTransitions() {
    onEach(
      profileModel.signOut.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }

    onEach(profileModel.signOut.jobFlow.successResults()) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.LogoutSuccessfully)
      }
    }

    onEach(intent(ViewIntents::logout)) {
      action { _, _, _ ->
        profileModel.signOut.start()
      }
    }
  }
}
