package ru.sla.clarify.feature.chronology.ui.screen.chronology

import me.tatarka.inject.annotations.Inject
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.plexus.core.FeatureConfigsManager
import ru.sla.clarify.core.domain.toggle.AppFeature
import ru.sla.clarify.core.domain.toggle.isFeatureEnabledLive
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.feature.chronology.domain.ChronologyModel
import ru.sla.clarify.feature.chronology.ui.routing.FlowEvent

class ChronologyViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val chronologyModel: ChronologyModel,
  private val featureConfigsManager: FeatureConfigsManager
) : ViewModel<ViewState, ViewIntents>() {

  private val episodes = mockNodes()

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState(
      nodes = episodes.map { it.node },
      episodeById = episodes.associateBy { it.node.id }
    ) to null

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ChronologyDismissed)
      }
    }

    onEach(chronologyModel.branches) {
      transitionTo { state, branches -> state.copy(branches = branches) }
    }

    configureDebugOverlay()
  }

  private fun MachineDsl<ViewState>.configureDebugOverlay() {
    onEach(featureConfigsManager.isFeatureEnabledLive(AppFeature.ChronologyDebugOverlay)) {
      transitionTo { state, enabled ->
        state.copy(debugOverlayAvailable = enabled)
      }
    }

    onEach(intent(ViewIntents::toggleDebugOverlay)) {
      transitionTo { state, _ ->
        state.copy(debugOverlayVisible = !state.debugOverlayVisible)
      }
    }
  }
}
