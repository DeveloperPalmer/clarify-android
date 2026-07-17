package ru.sla.clarify.feature.debug.panel.routing

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.core.routing.FlowNodeScopeDisposalHook
import ru.sla.clarify.feature.debug.panel.domain.di.DebugPanelScope
import ru.sla.clarify.feature.debug.panel.ui.routing.FlowEvent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope

class DebugPanelFlowNode @Inject constructor(
  @ForScope(DebugPanelScope::class)
  coroutineScope: CoroutineScope
) : BaseFlowNode<DebugPanelFlow.Result>() {

  init {
    addHook(FlowNodeScopeDisposalHook(coroutineScope))
  }

  override val dismissResult = DebugPanelFlow.Result.Dismissed
  override val initial = Target.debugPanelFlow.main

  override fun transition(event: Event): FlowTransition<DebugPanelFlow.Result> {
    return when (event) {
      is FlowEvent.DebugPanelDismissed -> Finish(DebugPanelFlow.Result.Dismissed)
      else -> Ignore
    }
  }
}
