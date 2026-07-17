package ru.sla.clarify.feature.debug.panel.routing

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.debug.panel.domain.DebugPanelModel
import ru.sla.clarify.feature.debug.panel.ui.routing.FlowEvent

class DebugPanelFlowNode @Inject constructor(
  private val debugPanelModel: DebugPanelModel
) : BaseFlowNode<DebugPanelFlow.Result>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = DebugPanelFlow.Result.Dismissed
  override val initial = Target.debugPanelFlow.main

  override fun onEntry(event: Event) {
    super.onEntry(event)
    debugPanelModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<DebugPanelFlow.Result> {
    return when (event) {
      is FlowEvent.DebugPanelDismissed -> Finish(DebugPanelFlow.Result.Dismissed)
      else -> Ignore
    }
  }
}
