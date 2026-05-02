package ru.kode.demo.app.routing.di

import com.squareup.anvil.annotations.ContributesBinding
import ru.kode.demo.app.routing.AppFlowNode
import ru.kode.demo.app.routing.AppFlowNodeBuilder
import ru.kode.demo.feature.main.routing.MainFlow
import ru.kode.way.FlowNode
import ru.kode.way.NodeBuilder
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
