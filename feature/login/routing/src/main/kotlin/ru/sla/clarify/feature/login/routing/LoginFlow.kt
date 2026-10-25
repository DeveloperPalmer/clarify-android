package ru.sla.clarify.feature.login.routing

import ru.sla.clarify.feature.login.routing.di.LoginFlowComponent

object LoginFlow {
  val schema = LoginFlowSchema()

  fun nodeBuilder(component: LoginFlowComponent): LoginFlowNodeBuilder {
    return LoginFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }

  enum class Result {
    Success,
    Dismissed
  }
}
