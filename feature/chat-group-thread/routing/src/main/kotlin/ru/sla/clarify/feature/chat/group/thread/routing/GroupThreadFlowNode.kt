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
import ru.sla.clarify.core.routing.FlowNodeScopeDisposalHook
import ru.sla.clarify.feature.chat.group.thread.domain.di.GroupThreadScope
import ru.sla.clarify.feature.chat.group.thread.ui.routing.FlowEvent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope

class GroupThreadFlowNode @Inject constructor(
  @ForScope(GroupThreadScope::class)
  coroutineScope: CoroutineScope
) : BaseFlowNode<GroupThreadFlow.Result>() {

  init {
    addHook(FlowNodeScopeDisposalHook(coroutineScope))
  }

  override val dismissResult = GroupThreadFlow.Result.Dismissed
  override val initial = Target.groupThreadFlow.main

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
