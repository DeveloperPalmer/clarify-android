package ru.sla.clarify.feature.chat.group.thread.routing

import ru.sla.clarify.feature.chat.group.thread.routing.di.GroupThreadFlowComponent

object GroupThreadFlow {
  val schema = GroupThreadFlowSchema()

  fun nodeBuilder(component: GroupThreadFlowComponent): GroupThreadFlowNodeBuilder {
    return GroupThreadFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }

  enum class Result {
    Dismissed
  }
}
