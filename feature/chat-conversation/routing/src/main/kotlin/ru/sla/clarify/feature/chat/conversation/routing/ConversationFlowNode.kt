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
import javax.inject.Inject

class ConversationFlowNode @Inject constructor(
  private val chatModel: ChatModel
) : BaseFlowNode<Unit>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = Unit
  override val initial = Target.conversationFlow.main

  override fun onEntry(event: Event) {
    super.onEntry(event)
    chatModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<Unit> {
    return when (event) {
      is FlowEvent.ChatListDismissed -> {
        Finish(Unit)
      }
      is ConversationFlowChildFinishRequest.ThreadFlow -> {
        NavigateTo(Target.conversationFlow.main)
      }
      is FlowEvent.ThreadRequested -> {
        NavigateTo(Target.conversationFlow.threadFlow(event.id))
      }
      else -> Ignore
    }
  }
}
