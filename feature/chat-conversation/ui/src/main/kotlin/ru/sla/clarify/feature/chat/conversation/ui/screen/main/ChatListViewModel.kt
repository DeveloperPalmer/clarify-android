package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.feature.chat.conversation.domain.ChatModel
import ru.sla.clarify.feature.chat.conversation.domain.PeerNotFoundException
import ru.sla.clarify.feature.chat.conversation.ui.routing.FlowEvent
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef
import javax.inject.Inject

class ChatListViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val chatModel: ChatModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to null

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

    onEach(intent(ViewIntents::openGroupChat)) {
      action { _, _, conversationId ->
        eventSink.sendEvent(FlowEvent.GroupThreadRequested(conversationId))
      }
    }

    onEach(intent(ViewIntents::openProfile)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ProfileRequested)
      }
    }

    configureUserTransitions()
    configureConversationTransitions()
    configureNewConversationTransitions()
    configureDeleteConversationTransitions()
  }

  private fun MachineDsl<ViewState>.configureUserTransitions() {
    onEach(chatModel.user) {
      transitionTo { state, user ->
        state.copy(user = user)
      }
    }
  }

  private fun MachineDsl<ViewState>.configureConversationTransitions() {
    onEach(chatModel.conversations) {
      transitionTo { state, conversations ->
        state.copy(conversations = conversations)
      }
    }
  }

  private fun MachineDsl<ViewState>.configureNewConversationTransitions() {
    onEach(intent(ViewIntents::showNewChatDialog)) {
      action { _, _, _ ->
        sendViewEvent(showNewChatDialog())
      }
    }

    onEach(intent(ViewIntents::confirmNewChat)) {
      action { _, _, value ->
        chatModel.getPeerByEmail.start(Email(value))
      }
    }

    onEach(chatModel.getPeerByEmail.jobFlow.successResults()) {
      action { _, _, peerId ->
        eventSink.sendEvent(FlowEvent.ThreadRequested(peerId))
      }
    }

    onEach(chatModel.getPeerByEmail.jobFlow.errors()) {
      action { _, _, error ->
        val messageId = when (error) {
          is PeerNotFoundException -> R.string.conversation_new_chat_error_user_not_found
          else -> R.string.conversation_new_chat_error_lookup_failed
        }
        sendViewEvent(
          Snackbar(
            isError = true,
            message = resRef(messageId)
          )
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configureDeleteConversationTransitions() {
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
}
