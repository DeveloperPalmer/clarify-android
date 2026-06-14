package ru.sla.clarify.feature.chat.conversation.routing.di

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
import ru.sla.clarify.feature.chat.group.thread.routing.GroupThreadFlow
import ru.sla.clarify.feature.profile.routing.ProfileFlow
import javax.inject.Inject
import javax.inject.Provider
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams as DirectTargetParams
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams as GroupTargetParams

class ConversationFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<ConversationFlowNode>,
  private val component: ConversationFlowComponent,
  @param:WiredScreen(Screen.Main)
  private val mainScreen: Provider<WiredComposableScreen>
) : ConversationFlowNodeBuilder.Factory {

  override fun createRootNode(): FlowNode<*> {
    return flowNode.get()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(mainScreen.get())
  }

  override fun createProfileFlowNodeBuilder(): NodeBuilder {
    return ProfileFlow.nodeBuilder(component.profileFlowComponent())
  }

  override fun createDirectThreadFlowNodeBuilder(params: DirectTargetParams): NodeBuilder {
    val component = component.directThreadFlowComponent()
      .params(params)
      .build()
    return DirectThreadFlow.nodeBuilder(component)
  }

  override fun createGroupThreadFlowNodeBuilder(params: GroupTargetParams): NodeBuilder {
    val component = component.groupThreadFlowComponent()
      .params(params)
      .build()
    return GroupThreadFlow.nodeBuilder(component)
  }
}
