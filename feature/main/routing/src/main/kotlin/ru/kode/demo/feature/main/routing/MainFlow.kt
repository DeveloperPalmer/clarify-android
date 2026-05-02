package ru.kode.demo.feature.main.routing

import ru.kode.demo.feature.main.routing.di.MainFlowComponent

object MainFlow {
  val schema = MainFlowSchema()

  fun nodeBuilder(component: MainFlowComponent): MainFlowNodeBuilder {
    return MainFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }
}
