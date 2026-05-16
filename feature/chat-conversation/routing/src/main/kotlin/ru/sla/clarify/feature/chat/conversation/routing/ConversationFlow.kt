package ru.sla.clarify.feature.chat.conversation.routing

import ru.sla.clarify.feature.chat.conversation.routing.di.ConversationFlowComponent
import ru.sla.clarify.feature.chat.thread.routing.ThreadFlow

object ConversationFlow {
  val schema = ConversationFlowSchema(
    threadFlowSchema = ThreadFlow.schema
  )

  fun nodeBuilder(component: ConversationFlowComponent): ConversationFlowNodeBuilder {
    return ConversationFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
