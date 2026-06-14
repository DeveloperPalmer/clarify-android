package ru.sla.clarify.feature.chat.direct.thread.routing

import ru.sla.clarify.feature.chat.branch.routing.BranchFlow
import ru.sla.clarify.feature.chat.direct.thread.routing.di.DirectThreadFlowComponent

object DirectThreadFlow {
  val schema = DirectThreadFlowSchema(
    branchFlowSchema = BranchFlow.schema
  )

  fun nodeBuilder(component: DirectThreadFlowComponent): DirectThreadFlowNodeBuilder {
    return DirectThreadFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
