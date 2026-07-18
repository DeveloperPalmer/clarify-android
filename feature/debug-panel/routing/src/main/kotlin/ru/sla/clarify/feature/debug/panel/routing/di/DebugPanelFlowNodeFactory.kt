package ru.sla.clarify.feature.debug.panel.routing.di

import me.tatarka.inject.annotations.Inject
import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.debug.panel.routing.DebugPanelFlowNode
import ru.sla.clarify.feature.debug.panel.routing.DebugPanelFlowNodeBuilder
import ru.sla.clarify.feature.debug.panel.ui.di.Screen
import ru.sla.clarify.feature.debug.panel.ui.di.WiredScreen

class DebugPanelFlowNodeFactory @Inject constructor(
  private val flowNode: () -> DebugPanelFlowNode,
  @param:WiredScreen(Screen.Main)
  private val mainScreenNode: () -> WiredComposableScreen,
  @param:WiredScreen(Screen.FeatureToggles)
  private val featureTogglesScreenNode: () -> WiredComposableScreen
) : DebugPanelFlowNodeBuilder.Factory {

  override fun createRootNode(): FlowNode<*> {
    return flowNode()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(mainScreenNode())
  }

  override fun createFeatureTogglesNode(): ScreenNode {
    return BasicScreenNode(featureTogglesScreenNode())
  }
}
