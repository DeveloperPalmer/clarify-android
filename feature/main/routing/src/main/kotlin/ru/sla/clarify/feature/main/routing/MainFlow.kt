package ru.sla.clarify.feature.main.routing

import ru.sla.clarify.feature.chat.conversation.routing.ConversationFlow
import ru.sla.clarify.feature.main.routing.di.MainFlowComponent

object MainFlow {
  val schema = MainFlowSchema(
    conversationFlowSchema = ConversationFlow.schema
  )

  fun nodeBuilder(component: MainFlowComponent): MainFlowNodeBuilder {
    return MainFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
