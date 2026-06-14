package ru.sla.clarify.feature.chat.group.thread.routing.di

import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams
import ru.sla.clarify.feature.chat.group.thread.routing.GroupThreadFlowNode
import ru.sla.clarify.feature.chat.group.thread.routing.GroupThreadFlowNodeBuilder
import ru.sla.clarify.feature.chat.group.thread.ui.di.Screen
import ru.sla.clarify.feature.chat.group.thread.ui.di.WiredScreen
import javax.inject.Inject
import javax.inject.Provider

class GroupThreadFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<GroupThreadFlowNode>,
  @param:WiredScreen(Screen.Main)
  private val mainScreenNode: Provider<WiredComposableScreen>,
  @param:WiredScreen(Screen.GroupInfo)
  private val groupInfoScreenNode: Provider<WiredComposableScreen>
) : GroupThreadFlowNodeBuilder.Factory {

  override fun createRootNode(params: TargetParams): FlowNode<*> {
    return flowNode.get()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(mainScreenNode.get())
  }

  override fun createGroupInfoNode(): ScreenNode {
    return BasicScreenNode(groupInfoScreenNode.get())
  }
}
