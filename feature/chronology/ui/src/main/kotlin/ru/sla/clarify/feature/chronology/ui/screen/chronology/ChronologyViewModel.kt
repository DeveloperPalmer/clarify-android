package ru.sla.clarify.feature.chronology.ui.screen.chronology

import kotlinx.coroutines.flow.map
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
import ru.sla.clarify.feature.chronology.ui.mapper.toChronology
import ru.sla.clarify.feature.chronology.ui.routing.FlowEvent

class ChronologyViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val chronologyModel: ChronologyModel,
  private val featureConfigsManager: FeatureConfigsManager
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to null

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ChronologyDismissed)
      }
    }

    onEach(chronologyModel.history.map { it.toChronology() }) {
      transitionTo { state, chronology ->
        state.copy(chronology = chronology)
      }
    }

    configureNodePreview()
    configureDebugOverlay()
  }

  private fun MachineDsl<ViewState>.configureNodePreview() {
    onEach(intent(ViewIntents::selectNode)) {
      transitionTo { state, selection ->
        state.copy(selectedNodeId = selection)
      }
    }

    onEach(intent(ViewIntents::closeNodePreview)) {
      transitionTo { state, _ ->
        state.copy(selectedNodeId = null)
      }
    }
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
