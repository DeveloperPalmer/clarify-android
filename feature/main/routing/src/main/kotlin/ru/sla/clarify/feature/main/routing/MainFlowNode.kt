package ru.sla.clarify.feature.main.routing

import kotlinx.coroutines.CoroutineScope
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.NavigateTo
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.main.domain.MainModel
import ru.sla.clarify.feature.main.ui.routing.FlowEvent
import javax.inject.Inject

class MainFlowNode @Inject constructor(
  private val mainModel: MainModel
) : BaseFlowNode<Unit>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = Unit
  override val initial = Target.mainFlow.main

  override fun onEntry(event: Event) {
    super.onEntry(event)
    mainModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<Unit> {
    return when (event) {
      is FlowEvent.LogoutSuccessfully -> {
        Finish(Unit)
      }
      is FlowEvent.OpenChats -> {
        NavigateTo(Target.mainFlow.chatFlow)
      }
      is MainFlowChildFinishRequest.ChatFlow -> {
        NavigateTo(Target.mainFlow.main)
      }
      else -> Ignore
    }
  }
}
