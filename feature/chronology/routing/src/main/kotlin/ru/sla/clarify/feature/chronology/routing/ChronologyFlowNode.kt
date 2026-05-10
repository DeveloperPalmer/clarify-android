package ru.sla.clarify.feature.chronology.routing

import kotlinx.coroutines.CoroutineScope
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.kode.way.whenFlowEvent
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.chronology.domain.ChronologyModel
import ru.sla.clarify.feature.chronology.ui.routing.FlowEvent
import javax.inject.Inject

class ChronologyFlowNode @Inject constructor(
  private val chronologyModel: ChronologyModel
) : BaseFlowNode<Unit>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = Unit
  override val initial = Target.chronologyFlow.chronology

  override fun onEntry(event: Event) {
    super.onEntry(event)
    chronologyModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<Unit> {
    return event.whenFlowEvent { e: FlowEvent ->
      when (e) {
        is FlowEvent.ChronologyDismissed -> Finish(Unit)
      }
    }
  }
}
