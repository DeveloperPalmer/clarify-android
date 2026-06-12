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
          selectedConversationsIds = emptyList()
        )
      }
      action { state, _, _ ->
        if (!state.editModeEnabled) {
          eventSink.sendEvent(FlowEvent.ChatListDismissed)
        }
      }
    }

    onEach(intent(ViewIntents::openDirectConversation)) {
      action { _, _, peerId ->
        eventSink.sendEvent(FlowEvent.DirectConversationRequested(peerId))
      }
    }

    onEach(intent(ViewIntents::openGroupConversation)) {
      action { _, _, conversationId ->
        eventSink.sendEvent(FlowEvent.GroupConversationRequested(conversationId))
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
    onEach(intent(ViewIntents::openCreateConversation)) {
      action { _, _, _ ->
        sendViewEvent(showNewChatDialog())
      }
    }

    onEach(intent(ViewIntents::changeCreateConversationTab)) {
      transitionTo { state, tab ->
        state.copy(selectedCreateConversationOption = tab)
      }
    }

    onEach(intent(ViewIntents::confirmCreateDirect)) {
      action { _, _, value ->
        chatModel.getPeerByEmail.start(Email(value))
      }
    }

    onEach(intent(ViewIntents::confirmCreateGroup)) {
      action { _, _, name ->
        chatModel.createGroup.start(name)
      }
    }

    onEach(chatModel.createGroup.jobFlow.successResults()) {
      action { _, _, conversationId ->
        eventSink.sendEvent(FlowEvent.GroupConversationRequested(conversationId))
      }
    }

    onEach(chatModel.createGroup.jobFlow.errors()) {
      action { _, _, _ ->
        sendViewEvent(
          Snackbar(
            isError = true,
            message = resRef(R.string.conversation_new_group_create_error)
          )
        )
      }
    }

    onEach(chatModel.getPeerByEmail.jobFlow.successResults()) {
      action { _, _, peerId ->
        eventSink.sendEvent(FlowEvent.DirectConversationRequested(peerId))
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
        val updated = if (state.selectedConversationsIds.contains(conversationId)) {
          state.selectedConversationsIds.minus(conversationId)
        } else {
          state.selectedConversationsIds.plus(conversationId)
        }
        state.copy(
          editModeEnabled = !state.editModeEnabled || updated.any { it != conversationId },
          selectedConversationsIds = updated
        )
      }
    }

    onEach(intent(ViewIntents::openDeleteConversation)) {
      action { _, _, _ ->
        sendViewEvent(showDeleteConversationDialog())
      }
    }

    onEach(intent(ViewIntents::confirmDeleteConversation)) {
      action { state, _, _ ->
        chatModel.deleteConversations.start(state.selectedConversationsIds)
      }
    }

    onEach(chatModel.deleteConversations.jobFlow.successResults()) {
      transitionTo { state, _ ->
        state.copy(
          editModeEnabled = false,
          selectedConversationsIds = emptyList()
        )
      }
    }
  }
}
