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
import ru.sla.clarify.feature.chronology.ui.mapper.toChronologyGraph
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

    configureNodePreview()

    // Граф собирается в потоке, а не в переходе: сборка обходит ленты всех веток, а переход
    // выполняется там же, где обновляется состояние экрана.
    //
    // Пустая история — не пустой экран и не ошибка: §13 брифа требует, чтобы переписка без веток и
    // даже без сообщений выглядела осмысленно, поэтому состояние загрузки здесь и не заводится.
    onEach(chronologyModel.history.map { it.toChronologyGraph() }) {
      transitionTo { state, graph -> state.copy(graph = graph) }
    }

    configureDebugOverlay()
  }

  /**
   * Выбор узла: что открыто и откуда оно выросло.
   *
   * Прямоугольник плашки приходит сюда готовым, из экрана, и это осознанная цена: пиксели во
   * `ViewState` выглядят чужеродно, но «какой узел выбран» и «откуда морфится карточка» — один факт,
   * и разложенный по двум домам он разъехался бы молча.
   */
  private fun MachineDsl<ViewState>.configureNodePreview() {
    onEach(intent(ViewIntents::selectNode)) {
      transitionTo { state, selection ->
        state.copy(selectedNode = selection)
      }
    }

    onEach(intent(ViewIntents::closeNodePreview)) {
      transitionTo { state, _ ->
        state.copy(selectedNode = null)
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
