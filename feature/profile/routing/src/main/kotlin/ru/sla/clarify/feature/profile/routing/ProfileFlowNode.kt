package ru.sla.clarify.feature.profile.routing

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.profile.domain.ProfileModel
import ru.sla.clarify.feature.profile.ui.routing.FlowEvent

class ProfileFlowNode @Inject constructor(
  private val profileModel: ProfileModel
) : BaseFlowNode<ProfileFlow.Result>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = ProfileFlow.Result.Dismissed
  override val initial = Target.profileFlow.main

  override fun onEntry(event: Event) {
    super.onEntry(event)
    profileModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<ProfileFlow.Result> {
    return when (event) {
      is FlowEvent.LogoutSuccessfully -> Finish(ProfileFlow.Result.LogoutSuccessfully)
      is FlowEvent.ProfileDismissed -> Finish(ProfileFlow.Result.Dismissed)
      else -> Ignore
    }
  }
}
