package ru.sla.clarify.feature.chat.ui.screen.list

import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.remo.errors
import ru.kode.way.Back
import ru.kode.way.Event
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.mapper.toAppUiError
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.domain.ChatListModel
import ru.sla.clarify.feature.chat.domain.ChatThreadModel
import ru.sla.clarify.feature.chat.ui.routing.FlowEvent
import javax.inject.Inject

class ChatListViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val chatListModel: ChatListModel,
  private val chatThreadModel: ChatThreadModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState(myUserId = chatListModel.currentUserId) to {
      chatListModel.refresh.start()
    }

    onEach(chatListModel.conversations) {
      transitionTo { state, list ->
        state.copy(conversations = list)
      }
    }

    onEach(
      chatListModel.refresh.jobFlow
        .asLceState(replayLastResult = true)
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }

    onEach(chatListModel.refresh.jobFlow.errors()) {
      transitionTo { state, error ->
        state.copy(snackbarError = error.toAppUiError())
      }
    }

    onEach(intent(ViewIntents::openChat)) {
      action { _, _, peerId ->
        chatThreadModel.savePeerId(peerId)
        eventSink.sendEvent(FlowEvent.OpenChatThread)
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
      action { state, _, _ ->
        chatThreadModel.savePeerId(state.peerIdInput.trim())
        eventSink.sendEvent(FlowEvent.OpenChatThread)
      }
      transitionTo { state, _ ->
        state.copy(newChatDialogVisible = false, peerIdInput = "")
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
