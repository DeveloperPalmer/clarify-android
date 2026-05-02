package ru.kode.demo.app.routing

import ru.kode.demo.app.routing.di.AppFlowComponent
import ru.kode.demo.feature.main.routing.MainFlow

object AppFlow {
  val schema = AppFlowSchema(MainFlow.schema)

  fun nodeBuilder(component: AppFlowComponent): AppFlowNodeBuilder {
    return AppFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
