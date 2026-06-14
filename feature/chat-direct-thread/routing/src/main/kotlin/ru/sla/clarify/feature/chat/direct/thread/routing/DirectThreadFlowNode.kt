package ru.sla.clarify.feature.chat.direct.thread.routing

import kotlinx.coroutines.CoroutineScope
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.NavigateTo
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadModel
import ru.sla.clarify.feature.chat.direct.thread.ui.routing.FlowEvent
import ru.sla.clarify.feature.entity.chat.Branch
import javax.inject.Inject
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams as BranchTargetParams

class DirectThreadFlowNode @Inject constructor(
  private val directThreadModel: DirectThreadModel
) : BaseFlowNode<Unit>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = Unit
  override val initial = Target.directThreadFlow.main

  override fun onEntry(event: Event) {
    super.onEntry(event)
    directThreadModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<Unit> {
    return when (event) {
      is FlowEvent.ThreadDismissed -> {
        Finish(Unit)
      }
      is FlowEvent.BranchRequested -> {
        NavigateTo(buildBranchFlow(event.branchId))
      }
      is DirectThreadFlowChildFinishRequest.BranchFlow -> {
        NavigateTo(Target.directThreadFlow.main)
      }
      else -> Ignore
    }
  }
}

private fun buildBranchFlow(branchId: Branch.Id): Target {
  val params = BranchTargetParams(branchId)
  return Target.directThreadFlow.branchFlow(params)
}
