package ru.sla.clarify.feature.chat.routing

import ru.sla.clarify.feature.chat.routing.di.ChatFlowComponent
import ru.sla.clarify.feature.chronology.routing.ChronologyFlow

object ChatFlow {
  val schema = ChatFlowSchema(
    chronologyFlowSchema = ChronologyFlow.schema
  )

  fun nodeBuilder(component: ChatFlowComponent): ChatFlowNodeBuilder {
    return ChatFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
