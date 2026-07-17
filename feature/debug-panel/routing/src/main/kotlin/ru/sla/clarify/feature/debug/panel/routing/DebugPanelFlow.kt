package ru.sla.clarify.feature.debug.panel.routing

import ru.sla.clarify.feature.debug.panel.routing.di.DebugPanelFlowComponent

object DebugPanelFlow {
  val schema = DebugPanelFlowSchema()

  fun nodeBuilder(component: DebugPanelFlowComponent): DebugPanelFlowNodeBuilder {
    return DebugPanelFlowNodeBuilder(
      nodeFactory = component.nodeFactory,
      schema = schema
    )
  }

  enum class Result {
    Dismissed
  }
}
