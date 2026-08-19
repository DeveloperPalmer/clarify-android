package ru.sla.clarify.feature.chronology.routing.di

import me.tatarka.inject.annotations.Inject
import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chronology.domain.entity.TargetParams
import ru.sla.clarify.feature.chronology.routing.ChronologyFlowNode
import ru.sla.clarify.feature.chronology.routing.ChronologyFlowNodeBuilder

class ChronologyFlowNodeFactory @Inject constructor(
  private val flowNode: () -> ChronologyFlowNode,
  private val chronologyScreenNode: () -> WiredComposableScreen
) : ChronologyFlowNodeBuilder.Factory {

  override fun createRootNode(params: TargetParams): FlowNode<*> {
    return flowNode()
  }

  override fun createChronologyNode(): ScreenNode {
    return BasicScreenNode(chronologyScreenNode())
  }
}
