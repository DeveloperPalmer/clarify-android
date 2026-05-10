package ru.sla.clarify.feature.chat.ui.screen.list

import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.way.Back
import ru.kode.way.Event
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.feature.chat.domain.ChatModel
import ru.sla.clarify.feature.chat.ui.routing.FlowEvent
import javax.inject.Inject

class ChatListViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val chatModel: ChatModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState(myUserId = chatModel.currentUserId) to null

    onEach(chatModel.conversations) {
      transitionTo { state, list ->
        state.copy(conversations = list)
      }
    }

    onEach(intent(ViewIntents::openChat)) {
      action { _, _, peerId ->
        eventSink.sendEvent(FlowEvent.ChatThreadRequested(peerId))
      }
    }

    onEach(intent(ViewIntents::showNewChatDialog)) {
      transitionTo { state, _ ->
        state.copy(newChatDialogVisible = true, peerIdInput = "")
      }
    }

    onEach(intent(ViewIntents::dismissNewChatDialog)) {
      transitionTo { state, _ ->
        state.copy(newChatDialogVisible = false)
      }
    }

    onEach(intent(ViewIntents::peerIdChanged)) {
      transitionTo { state, value ->
        state.copy(peerIdInput = value)
      }
    }

    onEach(intent(ViewIntents::confirmNewChat)) {
      transitionTo { state, _ ->
        state.copy(newChatDialogVisible = false, peerIdInput = "")
      }
      action { state, _, _ ->
        eventSink.sendEvent(FlowEvent.ChatThreadRequested(state.peerIdInput.trim()))
      }
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(Event.Back)
      }
    }

    onEach(intent(ViewIntents::dismissSnackbarError)) {
      transitionTo { state, _ ->
        state.copy(snackbarError = null)
      }
    }

    onEach(intent(ViewIntents::dismissDialogError)) {
      transitionTo { state, _ ->
        state.copy(dialogError = null)
      }
    }
  }
}
