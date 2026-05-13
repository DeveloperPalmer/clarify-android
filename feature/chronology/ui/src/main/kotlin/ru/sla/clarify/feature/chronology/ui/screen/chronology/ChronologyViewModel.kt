package ru.sla.clarify.feature.chronology.ui.screen.chronology

import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.chat.domain.ChatModel
import ru.sla.clarify.feature.chronology.domain.ChronologyModel
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import ru.sla.clarify.feature.chronology.ui.routing.FlowEvent
import javax.inject.Inject

@SingleIn(ChronologyScope::class)
class ChronologyViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val chronologyModel: ChronologyModel,
  private val chatModel: ChatModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    val peerId = chatModel.requirePeerId()
    initial = ViewState(peerId = peerId) to null

    onEach(chronologyModel.graph(peerId)) {
      transitionTo { state, graph -> state.copy(graph = graph) }
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ChronologyDismissed)
      }
    }
  }
}
