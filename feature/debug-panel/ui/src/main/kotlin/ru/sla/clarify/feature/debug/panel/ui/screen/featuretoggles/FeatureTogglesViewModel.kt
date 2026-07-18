package ru.sla.clarify.feature.debug.panel.ui.screen.featuretoggles

import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.plexus.core.FeatureConfigsManager
import ru.sla.clarify.core.domain.toggle.AppFeature
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.feature.debug.panel.domain.DebugPanelModel
import ru.sla.clarify.feature.debug.panel.ui.routing.FlowEvent

class FeatureTogglesViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val debugPanelModel: DebugPanelModel,
  private val featureConfigsManager: FeatureConfigsManager
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to null

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.FeatureTogglesDismissed)
      }
    }

    onEach(intent(ViewIntents::changeFeatureToggle)) {
      action { _, _, change ->
        debugPanelModel.setFeatureToggle.start(change)
      }
    }

    onEach(
      featureConfigsManager
        .getFeaturesValue(AppFeature.entries.map { it.key })
        .map { features ->
          AppFeature.entries.map { feature ->
            extractFeatureToggle(
              feature = feature,
              isEnabled = features[feature.key].toBoolean()
            )
          }
        }
    ) {
      transitionTo { state, featureToggles ->
        state.copy(featureToggles = featureToggles)
      }
    }
  }
}
