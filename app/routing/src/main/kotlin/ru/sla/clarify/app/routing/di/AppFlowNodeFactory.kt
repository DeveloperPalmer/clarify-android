package ru.sla.clarify.app.routing.di

import com.squareup.anvil.annotations.ContributesBinding
import ru.kode.way.FlowNode
import ru.kode.way.NodeBuilder
import ru.kode.way.ScreenNode
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.app.routing.AppFlowNode
import ru.sla.clarify.app.routing.AppFlowNodeBuilder
import ru.sla.clarify.app.routing.InitialFlowResolveNode
import ru.sla.clarify.feature.chat.conversation.routing.ConversationFlow
import ru.sla.clarify.feature.login.routing.LoginFlow
import javax.inject.Inject
import javax.inject.Provider

@ContributesBinding(AppFlowScope::class)
class AppFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<AppFlowNode>,
  private val component: AppFlowComponent
) : AppFlowNodeBuilder.Factory {

  override fun createRootNode(): FlowNode<*> {
    return flowNode.get()
  }

  override fun createConversationFlowNodeBuilder(): NodeBuilder {
    return ConversationFlow.nodeBuilder(component.conversationFlowComponent())
  }
  override fun createLoginFlowNodeBuilder(): NodeBuilder {
    return LoginFlow.nodeBuilder(component.loginFlowComponent())
  }

  override fun createInitialFlowResolveNode(): ScreenNode {
    return InitialFlowResolveNode()
  }
}
