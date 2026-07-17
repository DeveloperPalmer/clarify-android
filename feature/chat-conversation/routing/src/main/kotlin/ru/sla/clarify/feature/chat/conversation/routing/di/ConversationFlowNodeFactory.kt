package ru.sla.clarify.feature.chat.conversation.routing.di

import me.tatarka.inject.annotations.Inject
import ru.kode.way.FlowNode
import ru.kode.way.NodeBuilder
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.conversation.routing.ConversationFlowNode
import ru.sla.clarify.feature.chat.conversation.routing.ConversationFlowNodeBuilder
import ru.sla.clarify.feature.chat.conversation.ui.di.Screen
import ru.sla.clarify.feature.chat.conversation.ui.di.WiredScreen
import ru.sla.clarify.feature.chat.direct.thread.routing.DirectThreadFlow
import ru.sla.clarify.feature.chat.direct.thread.routing.di.DirectThreadFlowComponent
import ru.sla.clarify.feature.chat.group.thread.routing.GroupThreadFlow
import ru.sla.clarify.feature.chat.group.thread.routing.di.GroupThreadFlowComponent
import ru.sla.clarify.feature.profile.routing.ProfileFlow
import ru.sla.clarify.feature.profile.routing.di.ProfileFlowComponent
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams as DirectTargetParams
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams as GroupTargetParams

class ConversationFlowNodeFactory @Inject constructor(
  private val flowNode: () -> ConversationFlowNode,
  private val component: ConversationFlowComponent,
  @param:WiredScreen(Screen.Main)
  private val mainScreen: () -> WiredComposableScreen
) : ConversationFlowNodeBuilder.Factory {

  override fun createRootNode(): FlowNode<*> {
    return flowNode()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(mainScreen())
  }

  override fun createProfileFlowNodeBuilder(): NodeBuilder {
    val factory = component as ProfileFlowComponent.Factory
    return ProfileFlow.nodeBuilder(factory.createProfileFlowComponent())
  }

  override fun createDirectThreadFlowNodeBuilder(params: DirectTargetParams): NodeBuilder {
    val factory = component as DirectThreadFlowComponent.Factory
    return DirectThreadFlow.nodeBuilder(factory.createDirectThreadFlowComponent(params))
  }

  override fun createGroupThreadFlowNodeBuilder(params: GroupTargetParams): NodeBuilder {
    val factory = component as GroupThreadFlowComponent.Factory
    return GroupThreadFlow.nodeBuilder(factory.createGroupThreadFlowComponent(params))
  }
}
