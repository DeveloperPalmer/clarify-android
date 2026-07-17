package ru.sla.clarify.feature.login.routing

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.kode.way.whenFlowEvent
import ru.sla.clarify.core.routing.FlowNodeScopeDisposalHook
import ru.sla.clarify.feature.login.domain.LoginScope
import ru.sla.clarify.feature.login.ui.routing.FlowEvent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope

class LoginFlowNode @Inject constructor(
  @ForScope(LoginScope::class)
  coroutineScope: CoroutineScope
) : BaseFlowNode<LoginFlow.Result>() {

  init {
    addHook(FlowNodeScopeDisposalHook(coroutineScope))
  }

  override val dismissResult = LoginFlow.Result.Dismissed
  override val initial = Target.loginFlow.credentials

  override fun transition(event: Event): FlowTransition<LoginFlow.Result> {
    return event.whenFlowEvent { e: FlowEvent ->
      when (e) {
        FlowEvent.GoogleSignInSucceeded -> Finish(LoginFlow.Result.Success)
      }
    }
  }
}
