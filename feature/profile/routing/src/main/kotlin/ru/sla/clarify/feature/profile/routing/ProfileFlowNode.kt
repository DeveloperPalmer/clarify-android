package ru.sla.clarify.feature.profile.routing

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.core.routing.FlowNodeScopeDisposalHook
import ru.sla.clarify.feature.profile.domain.di.ProfileScope
import ru.sla.clarify.feature.profile.ui.routing.FlowEvent
import software.amazon.lastmile.kotlin.inject.anvil.ForScope

class ProfileFlowNode @Inject constructor(
  @ForScope(ProfileScope::class)
  coroutineScope: CoroutineScope
) : BaseFlowNode<ProfileFlow.Result>() {

  init {
    addHook(FlowNodeScopeDisposalHook(coroutineScope))
  }

  override val dismissResult = ProfileFlow.Result.Dismissed
  override val initial = Target.profileFlow.main

  override fun transition(event: Event): FlowTransition<ProfileFlow.Result> {
    return when (event) {
      is FlowEvent.LogoutSuccessfully -> Finish(ProfileFlow.Result.LogoutSuccessfully)
      is FlowEvent.ProfileDismissed -> Finish(ProfileFlow.Result.Dismissed)
      else -> Ignore
    }
  }
}
