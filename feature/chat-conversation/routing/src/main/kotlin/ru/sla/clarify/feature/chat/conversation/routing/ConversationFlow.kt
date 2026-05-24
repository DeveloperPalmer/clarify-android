package ru.sla.clarify.feature.chat.conversation.routing

import ru.sla.clarify.feature.chat.conversation.routing.di.ConversationFlowComponent
import ru.sla.clarify.feature.chat.thread.routing.ThreadFlow
import ru.sla.clarify.feature.profile.routing.ProfileFlow

object ConversationFlow {
  val schema = ConversationFlowSchema(
    threadFlowSchema = ThreadFlow.schema,
    profileFlowSchema = ProfileFlow.schema
  )

  fun nodeBuilder(component: ConversationFlowComponent): ConversationFlowNodeBuilder {
    return ConversationFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }

  enum class Result {
    LogoutSuccessfully,
    Dismissed
  }
}
