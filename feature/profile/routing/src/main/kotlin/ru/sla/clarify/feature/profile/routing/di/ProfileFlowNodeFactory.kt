package ru.sla.clarify.feature.profile.routing.di

import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.profile.routing.ProfileFlowNode
import ru.sla.clarify.feature.profile.routing.ProfileFlowNodeBuilder
import ru.sla.clarify.feature.profile.ui.di.Screen
import ru.sla.clarify.feature.profile.ui.di.WiredScreen
import javax.inject.Inject
import javax.inject.Provider

class ProfileFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<ProfileFlowNode>,
  @param:WiredScreen(Screen.Main)
  private val mainScreenNode: Provider<WiredComposableScreen>
) : ProfileFlowNodeBuilder.Factory {

  override fun createRootNode(): FlowNode<*> {
    return flowNode.get()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(mainScreenNode.get())
  }
}
