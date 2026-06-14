package ru.sla.clarify.feature.chat.conversation.routing

import kotlinx.coroutines.CoroutineScope
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.NavigateTo
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.chat.conversation.domain.ChatModel
import ru.sla.clarify.feature.chat.conversation.ui.routing.FlowEvent
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.ThreadTarget
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.feature.profile.routing.ProfileFlow
import javax.inject.Inject

class ConversationFlowNode @Inject constructor(
  private val chatModel: ChatModel
) : BaseFlowNode<ConversationFlow.Result>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = ConversationFlow.Result.Dismissed
  override val initial = Target.conversationFlow.main

  override fun onEntry(event: Event) {
    super.onEntry(event)
    chatModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<ConversationFlow.Result> {
    return when (event) {
      is FlowEvent.ChatListDismissed -> {
        Finish(ConversationFlow.Result.Dismissed)
      }
      is ConversationFlowChildFinishRequest.ThreadFlow -> {
        NavigateTo(Target.conversationFlow.main)
      }
      is FlowEvent.ProfileRequested -> {
        NavigateTo(Target.conversationFlow.profileFlow)
      }
      is FlowEvent.DirectConversationRequested -> {
        NavigateTo(buildDirectFlowFlow(event.id))
      }
      is FlowEvent.GroupConversationRequested -> {
        NavigateTo(Target.conversationFlow.groupThreadFlow(event.id))
      }
      is ConversationFlowChildFinishRequest.GroupThreadFlow -> {
        NavigateTo(Target.conversationFlow.main)
      }
      is ConversationFlowChildFinishRequest.ProfileFlow -> when (event.result) {
        ProfileFlow.Result.Dismissed -> {
          NavigateTo(Target.conversationFlow.main)
        }
        ProfileFlow.Result.LogoutSuccessfully -> {
          Finish(ConversationFlow.Result.LogoutSuccessfully)
        }
      }
      else -> Ignore
    }
  }
}

private fun buildDirectFlowFlow(id: Peer.Id): Target {
  val params = ThreadTarget(peerId = id)
  return Target.conversationFlow.threadFlow(params)
}
