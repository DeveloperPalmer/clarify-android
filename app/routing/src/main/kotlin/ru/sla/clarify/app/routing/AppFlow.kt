package ru.sla.clarify.app.routing

import ru.sla.clarify.app.routing.di.AppFlowComponent
import ru.sla.clarify.auth.session.domain.entity.AuthSessionState
import ru.sla.clarify.feature.chat.conversation.routing.ConversationFlow
import ru.sla.clarify.feature.login.routing.LoginFlow

object AppFlow {
  val schema = AppFlowSchema(
    conversationFlowSchema = ConversationFlow.schema,
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
