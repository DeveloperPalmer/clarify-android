package ru.sla.clarify.app.routing.di

import me.tatarka.inject.annotations.Inject
import ru.kode.way.FlowNode
import ru.kode.way.NodeBuilder
import ru.kode.way.ScreenNode
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.app.routing.AppFlowNode
import ru.sla.clarify.app.routing.AppFlowNodeBuilder
import ru.sla.clarify.app.routing.InitialFlowResolveNode
import ru.sla.clarify.feature.chat.conversation.routing.ConversationFlow
import ru.sla.clarify.feature.chat.conversation.routing.di.ConversationFlowComponent
import ru.sla.clarify.feature.login.routing.LoginFlow
import ru.sla.clarify.feature.login.routing.di.LoginFlowComponent
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@ContributesBinding(AppFlowScope::class)
class AppFlowNodeFactory @Inject constructor(
  private val flowNode: () -> AppFlowNode,
  private val appFlowComponent: AppFlowComponent
) : AppFlowNodeBuilder.Factory {

  override fun createRootNode(): FlowNode<*> {
    return flowNode()
  }

  override fun createConversationFlowNodeBuilder(): NodeBuilder {
    val factory = appFlowComponent as ConversationFlowComponent.Factory
    return ConversationFlow.nodeBuilder(factory.createConversationFlowComponent())
  }

  override fun createLoginFlowNodeBuilder(): NodeBuilder {
    val factory = appFlowComponent as LoginFlowComponent.Factory
    return LoginFlow.nodeBuilder(factory.createLoginFlowComponent())
  }

  override fun createInitialFlowResolveNode(): ScreenNode {
    return InitialFlowResolveNode()
  }
}
