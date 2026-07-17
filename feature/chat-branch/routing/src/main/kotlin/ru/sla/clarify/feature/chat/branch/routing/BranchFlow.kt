package ru.sla.clarify.feature.chat.branch.routing

import ru.sla.clarify.feature.chat.branch.routing.di.BranchFlowComponent

object BranchFlow {
  val schema = BranchFlowSchema()

  fun nodeBuilder(component: BranchFlowComponent): BranchFlowNodeBuilder {
    return BranchFlowNodeBuilder(
      nodeFactory = component.nodeFactory,
      schema = schema
    )
  }
}
