package ru.sla.clarify.feature.chat.direct.thread.routing.di

import me.tatarka.inject.annotations.Inject
import ru.kode.way.FlowNode
import ru.kode.way.NodeBuilder
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.branch.routing.BranchFlow
import ru.sla.clarify.feature.chat.branch.routing.di.BranchFlowComponent
import ru.sla.clarify.feature.chat.direct.thread.routing.DirectThreadFlowNode
import ru.sla.clarify.feature.chat.direct.thread.routing.DirectThreadFlowNodeBuilder
import ru.sla.clarify.feature.chat.direct.thread.ui.di.Screen
import ru.sla.clarify.feature.chat.direct.thread.ui.di.WiredScreen
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams as BranchTargetParams
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams as DirectTargetParams

class DirectThreadFlowNodeFactory @Inject constructor(
  private val flowNode: () -> DirectThreadFlowNode,
  private val component: DirectThreadFlowComponent,
  @param:WiredScreen(Screen.Thread)
  private val threadScreenNode: () -> WiredComposableScreen
) : DirectThreadFlowNodeBuilder.Factory {

  override fun createRootNode(params: DirectTargetParams): FlowNode<*> {
    return flowNode()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(threadScreenNode())
  }

  override fun createBranchFlowNodeBuilder(params: BranchTargetParams): NodeBuilder {
    val factory = component as BranchFlowComponent.Factory
    return BranchFlow.nodeBuilder(factory.createBranchFlowComponent(params))
  }
}
