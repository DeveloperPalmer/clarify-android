package ru.sla.clarify.app.routing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.NavigateTo
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.app.domain.di.AppFlowScope
import ru.sla.clarify.auth.session.domain.AuthSessionModel
import ru.sla.clarify.auth.session.domain.entity.AuthSessionState
import ru.sla.clarify.core.routing.FlowNodeScopeDisposalHook
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.chat.conversation.routing.ConversationFlow
import ru.sla.clarify.feature.login.routing.LoginFlow
import software.amazon.lastmile.kotlin.inject.anvil.ForScope

class AppFlowNode @Inject constructor(
  @param:ForScope(AppFlowScope::class)
  private val coroutineScope: CoroutineScope,
  private val eventSink: FlowEventSink,
  private val authSessionModel: AuthSessionModel
) : BaseFlowNode<Unit>() {

  init {
    addHook(FlowNodeScopeDisposalHook(coroutineScope))
  }

  override val dismissResult = Unit
  override val initial: Target = Target.appFlow.initialFlowResolve

  override fun onEntry(event: Event) {
    super.onEntry(event)
    coroutineScope.launch {
      val state = authSessionModel.sessionState.first()
      eventSink.sendEvent(AppFlow.Event.InitialSessionStateReceived(state))
    }
  }

  override fun transition(event: Event): FlowTransition<Unit> {
    return when (event) {
      is AppFlowChildFinishRequest.ConversationFlow -> {
        when (event.result) {
          ConversationFlow.Result.LogoutSuccessfully -> NavigateTo(Target.appFlow.loginFlow)
          ConversationFlow.Result.Dismissed -> Finish(Unit)
        }
      }
      is AppFlowChildFinishRequest.LoginFlow -> {
        when (event.result) {
          LoginFlow.Result.Success -> NavigateTo(Target.appFlow.conversationFlow)
          LoginFlow.Result.Dismissed -> Finish(Unit)
        }
      }
      is AppFlow.Event.InitialSessionStateReceived -> {
        when (event.state) {
          AuthSessionState.Active -> {
            NavigateTo(Target.appFlow.conversationFlow)
          }
          AuthSessionState.Inactive -> {
            NavigateTo(Target.appFlow.loginFlow)
          }
        }
      }
      else -> Ignore
    }
  }
}
