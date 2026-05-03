package ru.sla.clarify.app.routing

import ru.sla.clarify.app.routing.di.AppFlowComponent
import ru.sla.clarify.auth.session.domain.entity.AuthSessionState
import ru.sla.clarify.feature.login.routing.LoginFlow
import ru.sla.clarify.feature.main.routing.MainFlow

object AppFlow {
  val schema = AppFlowSchema(
    mainFlowSchema = MainFlow.schema,
    loginFlowSchema = LoginFlow.schema
  )

  fun nodeBuilder(component: AppFlowComponent): AppFlowNodeBuilder {
    return AppFlowNodeBuilder(
      nodeFactory = component.nodeFactory(),
      schema = schema
    )
  }

  sealed interface Event : ru.kode.way.Event {
    data class InitialSessionStateReceived(val state: AuthSessionState) : Event
  }
}
