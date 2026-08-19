package ru.sla.clarify.feature.chronology.routing

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.kode.way.whenFlowEvent
import ru.sla.clarify.core.routing.FlowNodeScopeDisposalHook
import ru.sla.clarify.feature.chronology.domain.di.ChronologyScope
import ru.sla.clarify.feature.chronology.ui.routing.FlowEvent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope

class ChronologyFlowNode @Inject constructor(
  @ForScope(ChronologyScope::class)
  coroutineScope: CoroutineScope
) : BaseFlowNode<Unit>() {

  init {
    addHook(FlowNodeScopeDisposalHook(coroutineScope))
  }

  override val dismissResult = Unit
  override val initial = Target.chronologyFlow.chronology

  override fun transition(event: Event): FlowTransition<Unit> {
    return event.whenFlowEvent { e: FlowEvent ->
      when (e) {
        is FlowEvent.ChronologyDismissed -> Finish(Unit)
      }
    }
  }
}
