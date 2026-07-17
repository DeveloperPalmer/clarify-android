package ru.sla.clarify.feature.chat.conversation.routing

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.NavigateTo
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.conversation.domain.ConversationModel
import ru.sla.clarify.feature.chat.conversation.ui.routing.FlowEvent
import ru.sla.clarify.feature.profile.routing.ProfileFlow
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.TargetParams as DirectTargetParams
import ru.sla.clarify.feature.chat.group.thread.domain.entity.TargetParams as GroupTargetParams

class ConversationFlowNode @Inject constructor(
  private val conversationModel: ConversationModel
) : BaseFlowNode<ConversationFlow.Result>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = ConversationFlow.Result.Dismissed
  override val initial = Target.conversationFlow.main

  override fun onEntry(event: Event) {
    super.onEntry(event)
    conversationModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<ConversationFlow.Result> {
    return when (event) {
      is FlowEvent.ChatListDismissed -> {
        Finish(ConversationFlow.Result.Dismissed)
      }
      is FlowEvent.ProfileRequested -> {
        NavigateTo(Target.conversationFlow.profileFlow)
      }
      is FlowEvent.DirectConversationRequested -> {
        NavigateTo(buildDirectFlow(event.id))
      }
      is FlowEvent.GroupConversationRequested -> {
        NavigateTo(buildGroupFlow(event.id))
      }
      is ConversationFlowChildFinishRequest.ProfileFlow -> when (event.result) {
        ProfileFlow.Result.Dismissed -> {
          NavigateTo(Target.conversationFlow.main)
        }
        ProfileFlow.Result.LogoutSuccessfully -> {
          Finish(ConversationFlow.Result.LogoutSuccessfully)
        }
      }
      is ConversationFlowChildFinishRequest.DirectThreadFlow -> {
        NavigateTo(Target.conversationFlow.main)
      }
      is ConversationFlowChildFinishRequest.GroupThreadFlow -> {
        NavigateTo(Target.conversationFlow.main)
      }
      else -> Ignore
    }
  }
}

private fun buildDirectFlow(id: Peer.Id): Target {
  val params = DirectTargetParams(id)
  return Target.conversationFlow.directThreadFlow(params)
}

private fun buildGroupFlow(id: Conversation.Id): Target {
  val params = GroupTargetParams(id)
  return Target.conversationFlow.groupThreadFlow(params)
}
