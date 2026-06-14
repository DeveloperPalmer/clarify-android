package ru.sla.clarify.feature.chat.direct.thread.routing.di

import ru.kode.way.FlowNode
import ru.kode.way.NodeBuilder
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.branch.routing.BranchFlow
import ru.sla.clarify.feature.chat.direct.thread.routing.DirectThreadFlowNode
import ru.sla.clarify.feature.chat.direct.thread.routing.DirectThreadFlowNodeBuilder
import ru.sla.clarify.feature.chat.direct.thread.ui.di.Screen
import ru.sla.clarify.feature.chat.direct.thread.ui.di.WiredScreen
import javax.inject.Inject
import javax.inject.Provider
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams as BranchTargetParams
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams as DirectTargetParams

class DirectThreadFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<DirectThreadFlowNode>,
  private val component: DirectThreadFlowComponent,
  @param:WiredScreen(Screen.Thread)
  private val threadScreenNode: Provider<WiredComposableScreen>
) : DirectThreadFlowNodeBuilder.Factory {

  override fun createRootNode(params: DirectTargetParams): FlowNode<*> {
    return flowNode.get()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(threadScreenNode.get())
  }

  override fun createBranchFlowNodeBuilder(params: BranchTargetParams): NodeBuilder {
    val branchComponent = component.branchFlowComponent()
      .params(params)
      .build()
    return BranchFlow.nodeBuilder(branchComponent)
  }
}
