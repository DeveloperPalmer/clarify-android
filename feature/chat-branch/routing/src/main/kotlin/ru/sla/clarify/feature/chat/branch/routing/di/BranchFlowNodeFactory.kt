package ru.sla.clarify.feature.chat.branch.routing.di

import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams
import ru.sla.clarify.feature.chat.branch.routing.BranchFlowNode
import ru.sla.clarify.feature.chat.branch.routing.BranchFlowNodeBuilder
import javax.inject.Inject
import javax.inject.Provider

class BranchFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<BranchFlowNode>,
  private val branchScreenNode: Provider<WiredComposableScreen>
) : BranchFlowNodeBuilder.Factory {

  override fun createRootNode(params: TargetParams): FlowNode<*> {
    return flowNode.get()
  }

  override fun createBranchNode(): ScreenNode {
    return BasicScreenNode(branchScreenNode.get())
  }
}
