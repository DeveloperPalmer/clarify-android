package ru.sla.clarify.feature.chat.direct.thread.routing

import ru.sla.clarify.feature.chat.branch.routing.BranchFlow
import ru.sla.clarify.feature.chat.direct.thread.routing.di.ThreadFlowComponent

object ThreadFlow {
  val schema = ThreadFlowSchema(
    branchFlowSchema = BranchFlow.schema
  )

  fun nodeBuilder(component: ThreadFlowComponent): ThreadFlowNodeBuilder {
    return ThreadFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
