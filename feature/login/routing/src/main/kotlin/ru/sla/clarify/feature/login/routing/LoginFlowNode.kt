package ru.sla.clarify.feature.login.routing

import me.tatarka.inject.annotations.Inject
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.kode.way.whenFlowEvent
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.login.domain.LoginModel
import ru.sla.clarify.feature.login.ui.routing.FlowEvent

class LoginFlowNode @Inject constructor(
  private val loginModel: LoginModel
) : BaseFlowNode<LoginFlow.Result>() {

  private val scope by FlowNodeCoroutineScopeHook()

  override val dismissResult = LoginFlow.Result.Dismissed
  override val initial = Target.loginFlow.credentials

  override fun onEntry(event: Event) {
    super.onEntry(event)
    loginModel.start(parentScope = scope)
  }

  override fun transition(event: Event): FlowTransition<LoginFlow.Result> {
    return event.whenFlowEvent { e: FlowEvent ->
      when (e) {
        FlowEvent.GoogleSignInSucceeded -> Finish(LoginFlow.Result.Success)
      }
    }
  }
}
