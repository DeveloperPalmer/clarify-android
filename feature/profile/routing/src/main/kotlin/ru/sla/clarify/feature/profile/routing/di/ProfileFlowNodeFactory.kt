package ru.sla.clarify.feature.profile.routing.di

import me.tatarka.inject.annotations.Inject
import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.profile.routing.ProfileFlowNode
import ru.sla.clarify.feature.profile.routing.ProfileFlowNodeBuilder
import ru.sla.clarify.feature.profile.ui.di.Screen
import ru.sla.clarify.feature.profile.ui.di.WiredScreen

class ProfileFlowNodeFactory @Inject constructor(
  private val flowNode: () -> ProfileFlowNode,
  @param:WiredScreen(Screen.Main)
  private val mainScreenNode: () -> WiredComposableScreen
) : ProfileFlowNodeBuilder.Factory {

  override fun createRootNode(): FlowNode<*> {
    return flowNode()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(mainScreenNode())
  }
}
