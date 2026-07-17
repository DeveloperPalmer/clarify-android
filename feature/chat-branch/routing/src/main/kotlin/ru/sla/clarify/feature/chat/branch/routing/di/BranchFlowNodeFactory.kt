package ru.sla.clarify.feature.chat.branch.routing.di

import me.tatarka.inject.annotations.Inject
import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams
import ru.sla.clarify.feature.chat.branch.routing.BranchFlowNode
import ru.sla.clarify.feature.chat.branch.routing.BranchFlowNodeBuilder

class BranchFlowNodeFactory @Inject constructor(
  private val flowNode: () -> BranchFlowNode,
  private val branchScreenNode: () -> WiredComposableScreen
) : BranchFlowNodeBuilder.Factory {

  override fun createRootNode(params: TargetParams): FlowNode<*> {
    return flowNode()
  }

  override fun createBranchNode(): ScreenNode {
    return BasicScreenNode(branchScreenNode())
  }
}
