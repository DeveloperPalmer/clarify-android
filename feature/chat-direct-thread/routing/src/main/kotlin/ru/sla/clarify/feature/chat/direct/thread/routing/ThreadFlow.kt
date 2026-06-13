package ru.sla.clarify.feature.chat.direct.thread.routing

import ru.sla.clarify.feature.chat.direct.thread.routing.di.ThreadFlowComponent

object ThreadFlow {
  val schema = ThreadFlowSchema()

  fun nodeBuilder(component: ThreadFlowComponent): ThreadFlowNodeBuilder {
    return ThreadFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
