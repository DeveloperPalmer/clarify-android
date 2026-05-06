package ru.sla.clarify.feature.chat.routing

import ru.sla.clarify.feature.chat.routing.di.ChatFlowComponent

object ChatFlow {
  val schema = ChatFlowSchema()

  fun nodeBuilder(component: ChatFlowComponent): ChatFlowNodeBuilder {
    return ChatFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
