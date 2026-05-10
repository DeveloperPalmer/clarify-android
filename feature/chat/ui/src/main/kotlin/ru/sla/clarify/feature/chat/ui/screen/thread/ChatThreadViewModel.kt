package ru.sla.clarify.feature.chat.ui.screen.thread

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.remo.JobState
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.mapper.toAppUiError
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.domain.ChatModel
import ru.sla.clarify.feature.chat.ui.routing.FlowEvent

class ChatThreadViewModel @AssistedInject constructor(
  private val eventSink: FlowEventSink,
  private val chatModel: ChatModel,
  @Assisted
  private val peerId: String
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState(peerId = peerId) to {
      chatModel.setPeerId(peerId)
      chatModel.markRead.start()
      chatModel.loadHistory.start()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ChatThreadDismissed)
      }
    }

    onEach(
      chatModel.loadHistory.jobFlow
        .asLceState(replayLastResult = true)
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }

    onEach(chatModel.loadHistory.jobFlow.successResults()) {
      transitionTo { state, history ->
        state.copy(messages = history.sortedBy { it.timestamp })
      }
    }

    onEach(chatModel.loadHistory.jobFlow.errors()) {
      transitionTo { state, error ->
        state.copy(snackbarError = error.toAppUiError())
      }
    }

    onEach(chatModel.treadMessages(peerId)) {
      transitionTo { state, message ->
        if (state.messages.any { it.msgId.isNotEmpty() && it.msgId == message.msgId }) {
          state
        } else {
          state.copy(messages = state.messages + message)
        }
      }
    }

    onEach(chatModel.sendText.jobFlow.state) {
      transitionTo { state, jobState ->
        state.copy(isSending = jobState == JobState.Running)
      }
    }

    onEach(chatModel.sendText.jobFlow.successResults()) {
      transitionTo { state, sent ->
        if (state.messages.any { it.msgId.isNotEmpty() && it.msgId == sent.msgId }) {
          state
        } else {
          state.copy(messages = state.messages + sent)
        }
      }
    }

    onEach(chatModel.sendText.jobFlow.errors()) {
      transitionTo { state, error ->
        state.copy(snackbarError = error.toAppUiError())
      }
    }

    onEach(intent(ViewIntents::inputChanged)) {
      transitionTo { state, value ->
        state.copy(inputValue = value)
      }
    }

    onEach(intent(ViewIntents::sendMessage)) {
      action { state, _, _ ->
        val text = state.inputValue.trim()
        if (text.isNotEmpty()) {
          chatModel.sendText.start(text)
        }
      }
      transitionTo { state, _ ->
        if (state.inputValue.isBlank()) state else state.copy(inputValue = "")
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
