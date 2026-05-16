package ru.sla.clarify.feature.chat.thread.routing.di

import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.thread.routing.ThreadFlowNode
import ru.sla.clarify.feature.chat.thread.routing.ThreadFlowNodeBuilder
import ru.sla.clarify.feature.chat.thread.ui.di.Screen
import ru.sla.clarify.feature.chat.thread.ui.di.WiredScreen
import ru.sla.clarify.feature.entity.chat.Peer
import javax.inject.Inject
import javax.inject.Provider

class ThreadFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<ThreadFlowNode>,
  @param:WiredScreen(Screen.Thread)
  private val threadScreenNode: Provider<WiredComposableScreen>
) : ThreadFlowNodeBuilder.Factory {

  override fun createRootNode(peerId: Peer.Id): FlowNode<*> {
    return flowNode.get()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(threadScreenNode.get())
  }
}
