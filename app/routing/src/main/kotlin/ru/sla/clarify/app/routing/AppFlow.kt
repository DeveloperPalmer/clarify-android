package ru.sla.clarify.app.routing

import ru.sla.clarify.app.routing.di.AppFlowComponent
import ru.sla.clarify.feature.main.routing.MainFlow

object AppFlow {
  val schema = AppFlowSchema(MainFlow.schema)

  fun nodeBuilder(component: AppFlowComponent): AppFlowNodeBuilder {
    return AppFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
