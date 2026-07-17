package ru.sla.clarify.feature.chat.group.thread.routing.di

import me.tatarka.inject.annotations.Inject
import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams
import ru.sla.clarify.feature.chat.group.thread.routing.GroupThreadFlowNode
import ru.sla.clarify.feature.chat.group.thread.routing.GroupThreadFlowNodeBuilder
import ru.sla.clarify.feature.chat.group.thread.ui.di.Screen
import ru.sla.clarify.feature.chat.group.thread.ui.di.WiredScreen

class GroupThreadFlowNodeFactory @Inject constructor(
  private val flowNode: () -> GroupThreadFlowNode,
  @param:WiredScreen(Screen.Main)
  private val mainScreenNode: () -> WiredComposableScreen,
  @param:WiredScreen(Screen.GroupInfo)
  private val groupInfoScreenNode: () -> WiredComposableScreen
) : GroupThreadFlowNodeBuilder.Factory {

  override fun createRootNode(params: TargetParams): FlowNode<*> {
    return flowNode()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(mainScreenNode())
  }

  override fun createGroupInfoNode(): ScreenNode {
    return BasicScreenNode(groupInfoScreenNode())
  }
}
