package ru.sla.clarify.feature.login.routing.di

import ru.kode.way.FlowNode
import ru.kode.way.ScreenNode
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.routing.BasicScreenNode
import ru.sla.clarify.core.ui.WiredComposableScreen
import ru.sla.clarify.feature.login.domain.LoginScope
import ru.sla.clarify.feature.login.routing.LoginFlowNode
import ru.sla.clarify.feature.login.routing.LoginFlowNodeBuilder
import ru.sla.clarify.feature.login.ui.di.Screen
import ru.sla.clarify.feature.login.ui.di.WiredScreen
import javax.inject.Inject
import javax.inject.Provider

@SingleIn(LoginScope::class)
class LoginFlowNodeFactory @Inject constructor(
  private val flowNode: Provider<LoginFlowNode>,
  @param:WiredScreen(Screen.Credentials)
  private val credentialsNode: Provider<WiredComposableScreen>
) : LoginFlowNodeBuilder.Factory {
  override fun createRootNode(): FlowNode<*> {
    return flowNode.get()
  }

  override fun createCredentialsNode(): ScreenNode {
    return BasicScreenNode(credentialsNode.get())
  }
}
