package ru.sla.clarify.feature.chronology.routing.di

import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chronology.routing.ChronologyFlowNode
import ru.sla.clarify.feature.chronology.routing.ChronologyFlowNodeBuilder
import ru.sla.clarify.feature.chronology.ui.di.Screen
import ru.sla.clarify.feature.chronology.ui.di.WiredScreen
import javax.inject.Inject
import javax.inject.Provider

class ChronologyFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<ChronologyFlowNode>,
  @param:WiredScreen(Screen.Chronology)
  private val chronologyScreenNode: Provider<WiredComposableScreen>
) : ChronologyFlowNodeBuilder.Factory {

  override fun createRootNode(): FlowNode<*> {
    return flowNode.get()
  }

  override fun createChronologyNode(): ScreenNode {
    return BasicScreenNode(chronologyScreenNode.get())
  }
}
