package ru.sla.clarify.app.routing.di

import com.squareup.anvil.annotations.ContributesBinding
import ru.kode.way.FlowNode
import ru.kode.way.NodeBuilder
import ru.sla.clarify.app.routing.AppFlowNode
import ru.sla.clarify.app.routing.AppFlowNodeBuilder
import ru.sla.clarify.feature.main.routing.MainFlow
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

  override fun createMainFlowNodeBuilder(): NodeBuilder {
    return MainFlow.nodeBuilder(component.mainFlowComponent())
  }
}
