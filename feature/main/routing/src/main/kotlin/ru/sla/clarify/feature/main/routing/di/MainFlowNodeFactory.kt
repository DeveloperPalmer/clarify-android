package ru.sla.clarify.feature.main.routing.di

import ru.kode.way.FlowNode
import ru.kode.way.NodeBuilder
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.routing.ChatFlow
import ru.sla.clarify.feature.main.routing.MainFlowNode
import ru.sla.clarify.feature.main.routing.MainFlowNodeBuilder
import ru.sla.clarify.feature.main.ui.di.Screen
import ru.sla.clarify.feature.main.ui.di.WiredScreen
import javax.inject.Inject
import javax.inject.Provider

class MainFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<MainFlowNode>,
  @WiredScreen(Screen.Main)
  private val mainScreenNode: Provider<WiredComposableScreen>,
  private val component: MainFlowComponent
) : MainFlowNodeBuilder.Factory {

  override fun createRootNode(): FlowNode<*> {
    return flowNode.get()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(mainScreenNode.get())
  }

  override fun createChatFlowNodeBuilder(): NodeBuilder {
    return ChatFlow.nodeBuilder(component.chatFlowComponent())
  }
}
