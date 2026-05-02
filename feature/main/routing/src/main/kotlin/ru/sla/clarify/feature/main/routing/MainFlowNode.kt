package ru.sla.clarify.feature.main.routing

import ru.kode.way.Event
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.kode.way.whenFlowEvent
import ru.sla.clarify.feature.main.ui.routing.FlowEvent
import javax.inject.Inject

class MainFlowNode @Inject constructor() : BaseFlowNode<Unit>() {
  override val dismissResult = Unit
  override val initial = Target.mainFlow.main

  override fun transition(event: Event): FlowTransition<Unit> {
    return event.whenFlowEvent { _: FlowEvent ->
      Ignore
    }
  }
}
