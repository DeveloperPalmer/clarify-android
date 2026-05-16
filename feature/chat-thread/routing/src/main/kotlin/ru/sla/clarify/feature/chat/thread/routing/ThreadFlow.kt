package ru.sla.clarify.feature.chat.thread.routing

import ru.sla.clarify.feature.chat.thread.routing.di.ThreadFlowComponent

object ThreadFlow {
  val schema = ThreadFlowSchema()

  fun nodeBuilder(component: ThreadFlowComponent): ThreadFlowNodeBuilder {
    return ThreadFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
