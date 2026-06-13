package ru.sla.clarify.feature.chat.branch.routing

import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.kode.way.whenFlowEvent
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.chat.branch.domain.BranchModel
import ru.sla.clarify.feature.chat.branch.ui.routing.FlowEvent
import javax.inject.Inject

class BranchFlowNode @Inject constructor(
  private val branchModel: Lazy<BranchModel>
) : BaseFlowNode<Unit>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = Unit
  override val initial = Target.branchFlow.branch

  override fun onEntry(event: Event) {
    super.onEntry(event)
    branchModel.get().start(scope)
  }

  override fun transition(event: Event): FlowTransition<Unit> {
    return event.whenFlowEvent { e: FlowEvent ->
      when (e) {
        is FlowEvent.BranchDismissed -> Finish(Unit)
      }
    }
  }
}
