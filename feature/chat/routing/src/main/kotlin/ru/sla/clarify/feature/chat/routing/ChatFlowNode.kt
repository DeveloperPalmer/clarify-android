package ru.sla.clarify.feature.chat.routing

import kotlinx.coroutines.CoroutineScope
import ru.kode.way.Event
import ru.kode.way.Finish
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.NavigateTo
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.chat.domain.ChatModel
import ru.sla.clarify.feature.chat.ui.routing.FlowEvent
import javax.inject.Inject

class ChatFlowNode @Inject constructor(
  private val chatModel: ChatModel
) : BaseFlowNode<Unit>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = Unit
  override val initial = Target.chatFlow.chatList

  override fun onEntry(event: Event) {
    super.onEntry(event)
    chatModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<Unit> {
    return when (event) {
      is FlowEvent.ChatListDismissed -> {
        Finish(Unit)
      }
      is ChatFlowChildFinishRequest.ChronologyFlow -> {
        NavigateTo(Target.chatFlow.chatThread(chatModel.requirePeerId()))
      }
      is FlowEvent.ChatThreadRequested -> {
        NavigateTo(Target.chatFlow.chatThread(event.peerId))
      }
      is FlowEvent.ChatThreadDismissed -> {
        NavigateTo(Target.chatFlow.chatList)
      }
      is FlowEvent.ChronologyRequested -> {
        NavigateTo(Target.chatFlow.chronologyFlow)
      }
      else -> Ignore
    }
  }
}
