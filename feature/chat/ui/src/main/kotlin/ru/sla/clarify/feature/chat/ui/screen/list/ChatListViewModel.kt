package ru.sla.clarify.feature.chat.ui.screen.list

import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.domain.ChatModel
import ru.sla.clarify.feature.chat.ui.routing.FlowEvent
import javax.inject.Inject

class ChatListViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val chatModel: ChatModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to {
      chatModel.fetchConversations.startOnSubscribe()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ChatListDismissed)
      }
    }

    onEach(intent(ViewIntents::openChat)) {
      action { _, _, peerId ->
        eventSink.sendEvent(FlowEvent.ChatThreadRequested(peerId))
      }
    }

    onEach(intent(ViewIntents::showNewChatDialog)) {
      transitionTo { state, _ ->
        state.copy(newChatDialogVisible = true)
      }
    }

    onEach(intent(ViewIntents::dismissNewChatDialog)) {
      transitionTo { state, _ ->
        state.copy(newChatDialogVisible = false)
      }
    }

    onEach(intent(ViewIntents::confirmNewChat)) {
      action { _, _, peerId ->
        if (peerId.isNotBlank()) {
          eventSink.sendEvent(FlowEvent.ChatThreadRequested(peerId.trim()))
        }
      }
    }

    configureFetchConversationTransitions()
    configureDeleteConversationTransitions()
  }

  private fun MachineDsl<ViewState>.configureDeleteConversationTransitions() {
    onEach(intent(ViewIntents::showDeleteMenu)) {
      transitionTo { state, conversationDeletionId ->
        state.copy(conversationDeletionId = conversationDeletionId)
      }
    }

    onEach(intent(ViewIntents::dismissDeleteMenu)) {
      transitionTo { state, _ ->
        state.copy(
          conversationDeletionId = null
        )
      }
    }

    onEach(intent(ViewIntents::showDeleteConfirmation)) {
      transitionTo { state, _ ->
        state.copy(
          conversationDeletionId = null
        )
      }
      action { state, _, _ ->
        val target = state.conversations.first { it.id == state.conversationDeletionId }
        sendViewEvent(
          showDeleteConversationDialog(
            peerLabel = target.peer.id,
            conversationId = target.id
          )
        )
      }
    }

    onEach(intent(ViewIntents::confirmDeleteConversation)) {
      action { state, _, id ->
        chatModel.deleteConversation.start(id)
      }
    }
  }

  private fun MachineDsl<ViewState>.configureFetchConversationTransitions() {
    onEach(chatModel.conversations) {
      transitionTo { state, conversations ->
        state.copy(
          myUserId = chatModel.currentUserId,
          conversations = conversations
        )
      }
    }

    onEach(
      chatModel.fetchConversations.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }
  }
}
