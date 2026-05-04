package ru.sla.clarify.app.routing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.NavigateTo
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.auth.session.domain.AuthSessionModel
import ru.sla.clarify.auth.session.domain.entity.AuthSessionState
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.login.routing.LoginFlow
import javax.inject.Inject

class AppFlowNode @Inject constructor(
  private val eventSink: FlowEventSink,
  private val authSessionModel: AuthSessionModel
) : BaseFlowNode<Unit>() {

  override val dismissResult = Unit
  override val initial: Target = Target.appFlow.initialFlowResolve
  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook<Unit>()

  override fun onEntry(event: Event) {
    super.onEntry(event)
    scope.launch {
      val state = authSessionModel.sessionState.first()
      eventSink.sendEvent(AppFlow.Event.InitialSessionStateReceived(state))
    }
  }

  override fun transition(event: Event): FlowTransition<Unit> {
    return when (event) {
      is AppFlowChildFinishRequest.MainFlow -> {
        NavigateTo(Target.appFlow.loginFlow)
      }
      is AppFlowChildFinishRequest.LoginFlow -> {
        when (event.result) {
          LoginFlow.Result.Success -> NavigateTo(Target.appFlow.mainFlow)
          LoginFlow.Result.Dismissed -> Finish(Unit)
        }
      }
      is AppFlow.Event.InitialSessionStateReceived -> {
        when (event.state) {
          AuthSessionState.Active -> {
            NavigateTo(Target.appFlow.mainFlow)
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
