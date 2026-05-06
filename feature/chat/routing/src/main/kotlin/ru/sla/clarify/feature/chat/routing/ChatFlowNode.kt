package ru.sla.clarify.feature.chat.routing

import kotlinx.coroutines.CoroutineScope
import ru.kode.way.Back
import ru.kode.way.Event
import ru.kode.way.FlowTransition
import ru.kode.way.Ignore
import ru.kode.way.NavigateTo
import ru.kode.way.Target
import ru.kode.way.extension.node.hook.BaseFlowNode
import ru.kode.way.whenFlowEvent
import ru.sla.clarify.core.routing.FlowNodeCoroutineScopeHook
import ru.sla.clarify.feature.chat.domain.ChatListModel
import ru.sla.clarify.feature.chat.domain.ChatNavState
import ru.sla.clarify.feature.chat.domain.ChatThreadModel
import ru.sla.clarify.feature.chat.ui.routing.ChatFlowEvent
import javax.inject.Inject

class ChatFlowNode @Inject constructor(
  private val chatNavState: ChatNavState,
  private val chatListModel: ChatListModel,
  private val chatThreadModel: ChatThreadModel
) : BaseFlowNode<Unit>() {

  private val scope: CoroutineScope by FlowNodeCoroutineScopeHook<Unit>()

  override val dismissResult = Unit
  override val initial = Target.chatFlow.chatList

  // Tracks whether the chat-thread screen is currently active. Toggled in transition() so
  // that pressing the system back button on the thread screen returns to the list screen,
  // while pressing it on the list screen falls through to the framework's default handling
  // (which finishes this flow).
  private var atThread = false

  override fun onEntry(event: Event) {
    super.onEntry(event)
    chatListModel.start(scope)
    chatThreadModel.start(scope)
  }

  override fun transition(event: Event): FlowTransition<Unit> {
    val flowEventTransition = event.whenFlowEvent { e: ChatFlowEvent ->
      when (e) {
        is ChatFlowEvent.OpenChat -> {
          chatNavState.peerUserId = e.peerUserId
          atThread = true
          NavigateTo(Target.chatFlow.chatThread)
        }
      }
    }
    if (flowEventTransition !== Ignore) return flowEventTransition
    if (event == Event.Back && atThread) {
      atThread = false
      return NavigateTo(Target.chatFlow.chatList)
    }
    return Ignore
  }
}
