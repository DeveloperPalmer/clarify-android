package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.conversation.domain.ChatModel
import ru.sla.clarify.feature.chat.conversation.ui.routing.FlowEvent
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
      transitionTo { state, _ ->
        state.copy(
          editModeEnabled = false,
          selectedConversationIds = emptyList()
        )
      }
      action { state, _, _ ->
        if (!state.editModeEnabled) {
          eventSink.sendEvent(FlowEvent.ChatListDismissed)
        }
      }
    }

    onEach(intent(ViewIntents::openChat)) {
      action { _, _, peerId ->
        eventSink.sendEvent(FlowEvent.ThreadRequested(peerId))
      }
    }

    onEach(intent(ViewIntents::showNewChatDialog)) {
      action { _, _, _ ->
        sendViewEvent(showNewChatDialog())
      }
    }

    onEach(intent(ViewIntents::confirmNewChat)) {
      action { _, _, peerId ->
        if (peerId.value.isNotBlank()) {
          eventSink.sendEvent(FlowEvent.ThreadRequested(peerId))
        }
      }
    }

    configureFetchConversationTransitions()
    configureDeleteConversationTransitions()
  }

  private fun MachineDsl<ViewState>.configureDeleteConversationTransitions() {
    onEach(intent(ViewIntents::openSettings)) {
      action { _, _, _ ->
        sendViewEvent(showConversationOptions())
      }
    }

    onEach(intent(ViewIntents::handleConversationLongPress)) {
      transitionTo { state, conversationId ->
        val updated = if (state.selectedConversationIds.contains(conversationId)) {
          state.selectedConversationIds.minus(conversationId)
        } else {
          state.selectedConversationIds.plus(conversationId)
        }
        state.copy(
          editModeEnabled = !state.editModeEnabled || updated.any { it != conversationId },
          selectedConversationIds = updated
        )
      }
    }

    onEach(intent(ViewIntents::showDeleteConfirmation)) {
      action { _, _, _ ->
        sendViewEvent(showDeleteConversationDialog())
      }
    }

    onEach(intent(ViewIntents::confirmDeleteConversation)) {
      action { state, _, _ ->
        chatModel.deleteConversations.start(state.selectedConversationIds)
      }
    }

    onEach(chatModel.deleteConversations.jobFlow.successResults()) {
      transitionTo { state, _ ->
        state.copy(
          editModeEnabled = false,
          selectedConversationIds = emptyList()
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configureFetchConversationTransitions() {
    onEach(chatModel.userId) {
      transitionTo { state, userId ->
        state.copy(userId = userId.value)
      }
    }

    onEach(chatModel.conversations) {
      transitionTo { state, conversations ->
        state.copy(conversations = conversations)
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
