package ru.sla.clarify.feature.chat.direct.thread.routing

import ru.sla.clarify.feature.chat.branch.routing.BranchFlow
import ru.sla.clarify.feature.chat.direct.thread.routing.di.DirectThreadFlowComponent
import ru.sla.clarify.feature.chronology.routing.ChronologyFlow

object DirectThreadFlow {
  val schema = DirectThreadFlowSchema(
    branchFlowSchema = BranchFlow.schema,
    chronologyFlowSchema = ChronologyFlow.schema
  )

  fun nodeBuilder(component: DirectThreadFlowComponent): DirectThreadFlowNodeBuilder {
    return DirectThreadFlowNodeBuilder(
      nodeFactory = component.nodeFactory,
      schema = schema
    )
  }
}
