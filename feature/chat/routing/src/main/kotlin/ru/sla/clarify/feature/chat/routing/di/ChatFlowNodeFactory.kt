package ru.sla.clarify.feature.chat.routing.di

import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.routing.ChatFlowNode
import ru.sla.clarify.feature.chat.routing.ChatFlowNodeBuilder
import ru.sla.clarify.feature.chat.ui.di.Screen
import ru.sla.clarify.feature.chat.ui.di.WiredComposableScreenFactory
import ru.sla.clarify.feature.chat.ui.di.WiredScreen
import javax.inject.Inject
import javax.inject.Provider

class ChatFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<ChatFlowNode>,
  @param:WiredScreen(Screen.ChatList)
  private val chatListScreen: Provider<WiredComposableScreen>,
  @param:WiredScreen(Screen.ChatThread)
  private val chatThreadScreen: Provider<WiredComposableScreenFactory>
) : ChatFlowNodeBuilder.Factory {

  override fun createRootNode(): FlowNode<*> {
    return flowNode.get()
  }

  override fun createChatListNode(): ScreenNode {
    return BasicScreenNode(chatListScreen.get())
  }

  override fun createChatThreadNode(peerId: String): ScreenNode {
    return BasicScreenNode(chatThreadScreen.get().create(peerId))
  }
}
