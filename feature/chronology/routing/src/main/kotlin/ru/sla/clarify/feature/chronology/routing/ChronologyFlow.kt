package ru.sla.clarify.feature.chronology.routing

import ru.sla.clarify.feature.chronology.routing.di.ChronologyFlowComponent

object ChronologyFlow {
  val schema = ChronologyFlowSchema()

  fun nodeBuilder(component: ChronologyFlowComponent): ChronologyFlowNodeBuilder {
    return ChronologyFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
