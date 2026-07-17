package ru.sla.clarify.feature.profile.routing

import ru.sla.clarify.feature.profile.routing.di.ProfileFlowComponent

object ProfileFlow {
  val schema = ProfileFlowSchema()

  fun nodeBuilder(component: ProfileFlowComponent): ProfileFlowNodeBuilder {
    return ProfileFlowNodeBuilder(
      nodeFactory = component.nodeFactory,
      schema = schema
    )
  }

  enum class Result {
    LogoutSuccessfully,
    Dismissed
  }
}
