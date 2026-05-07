package ru.sla.clarify.feature.chat.routing

import kotlinx.coroutines.CoroutineScope
import ru.kode.way.Event
import ru.kode.way.FlowTransition
import ru.kode.way.NavigateTo
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.kode.way.whenFlowEvent
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.chat.domain.ChatListModel
import ru.sla.clarify.feature.chat.domain.ChatThreadModel
import ru.sla.clarify.feature.chat.ui.routing.FlowEvent
import javax.inject.Inject

class ChatFlowNode @Inject constructor(
  private val chatListModel: ChatListModel,
  private val chatThreadModel: ChatThreadModel
) : BaseFlowNode<Unit>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook()

  override val dismissResult = Unit
  override val initial = Target.chatFlow.chatList

  override fun onEntry(event: Event) {
    super.onEntry(event)
    chatListModel.start(scope)
    chatThreadModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<Unit> {
    return event.whenFlowEvent { e: FlowEvent ->
      when (e) {
        is FlowEvent.OpenChatThread -> {
          NavigateTo(Target.chatFlow.chatThread)
        }
      }
    }
  }
}
