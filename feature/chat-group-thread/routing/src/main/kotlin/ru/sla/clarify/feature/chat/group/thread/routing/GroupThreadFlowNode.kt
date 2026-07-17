package ru.sla.clarify.feature.chat.group.thread.routing

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.NavigateTo
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.chat.group.thread.domain.GroupThreadModel
import ru.sla.clarify.feature.chat.group.thread.ui.routing.FlowEvent

class GroupThreadFlowNode @Inject constructor(
  private val groupThreadModel: GroupThreadModel
) : BaseFlowNode<GroupThreadFlow.Result>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = GroupThreadFlow.Result.Dismissed
  override val initial = Target.groupThreadFlow.main

  override fun onEntry(event: Event) {
    super.onEntry(event)
    groupThreadModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<GroupThreadFlow.Result> {
    return when (event) {
      is FlowEvent.GroupThreadDismissed -> Finish(GroupThreadFlow.Result.Dismissed)
      is FlowEvent.GroupInfoRequested -> NavigateTo(Target.groupThreadFlow.groupInfo)
      is FlowEvent.GroupInfoDismissed -> NavigateTo(Target.groupThreadFlow.main)
      is FlowEvent.GroupClosed -> Finish(GroupThreadFlow.Result.Dismissed)
      else -> Ignore
    }
  }
}
