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
import ru.sla.clarify.feature.chat.branch.domain.entity.Branch
import ru.sla.clarify.feature.chat.direct.thread.domain.ThreadModel
import ru.sla.clarify.feature.chat.direct.thread.ui.routing.FlowEvent
import javax.inject.Inject

class ThreadFlowNode @Inject constructor(
  private val threadModel: ThreadModel
) : BaseFlowNode<Unit>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = Unit
  override val initial = Target.threadFlow.thread

  override fun onEntry(event: Event) {
    super.onEntry(event)
    threadModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<Unit> {
    return when (event) {
      is FlowEvent.ThreadDismissed -> Finish(Unit)
      is FlowEvent.BranchRequested -> NavigateTo(
        Target.threadFlow.branchFlow(Branch.Id(event.branchId.value))
      )
      is ThreadFlowChildFinishRequest.BranchFlow -> NavigateTo(Target.threadFlow.thread)
      else -> Ignore
    }
  }
}
