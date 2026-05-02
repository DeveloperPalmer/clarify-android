package ru.kode.demo.feature.main.routing.di

import ru.kode.demo.core.routing.BasicScreenNode
import ru.kode.demo.core.ui.WiredComposableScreen
import ru.kode.demo.feature.main.routing.MainFlowNode
import ru.kode.demo.feature.main.routing.MainFlowNodeBuilder
import ru.kode.demo.feature.main.ui.di.Screen
import ru.kode.demo.feature.main.ui.di.WiredScreen
import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import javax.inject.Inject
import javax.inject.Provider

class MainFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<MainFlowNode>,
  @WiredScreen(Screen.Main)
  private val mainScreenNode: Provider<WiredComposableScreen>
) : MainFlowNodeBuilder.Factory {

  override fun createRootNode(): FlowNode<*> {
    return flowNode.get()
  }

  override fun createMainNode(): ScreenNode {
    return BasicScreenNode(mainScreenNode.get())
  }
}
