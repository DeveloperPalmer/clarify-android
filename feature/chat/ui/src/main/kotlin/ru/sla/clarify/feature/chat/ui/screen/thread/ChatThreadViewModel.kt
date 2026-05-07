package ru.sla.clarify.feature.chat.ui.screen.thread

import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.machine
import ru.kode.amvi.viewmodel.ViewModel
import ru.kode.remo.JobState
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.kode.way.Back
import ru.kode.way.Event
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.mapper.toAppUiError
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.domain.ChatThreadModel
import javax.inject.Inject

class ChatThreadViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val chatThreadModel: ChatThreadModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState(
      peerId = chatThreadModel.readPeerId()
    ) to {
      chatThreadModel.loadHistory.start()
      chatThreadModel.markRead.start()
    }

    onEach(
      chatThreadModel.loadHistory.jobFlow
        .asLceState(replayLastResult = true)
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }

    onEach(chatThreadModel.loadHistory.jobFlow.successResults()) {
      transitionTo { state, history ->
        state.copy(messages = history.sortedBy { it.timestamp })
      }
    }

    onEach(chatThreadModel.loadHistory.jobFlow.errors()) {
      transitionTo { state, error ->
        state.copy(snackbarError = error.toAppUiError())
      }
    }

    onEach(chatThreadModel.treadMessages) {
      transitionTo { state, message ->
        if (state.messages.any { it.msgId.isNotEmpty() && it.msgId == message.msgId }) {
          state
        } else {
          state.copy(messages = state.messages + message)
        }
      }
    }

    onEach(chatThreadModel.sendText.jobFlow.state) {
      transitionTo { state, jobState ->
        state.copy(isSending = jobState == JobState.Running)
      }
    }

    onEach(chatThreadModel.sendText.jobFlow.successResults()) {
      transitionTo { state, sent ->
        if (state.messages.any { it.msgId.isNotEmpty() && it.msgId == sent.msgId }) {
          state
        } else {
          state.copy(messages = state.messages + sent)
        }
      }
    }

    onEach(chatThreadModel.sendText.jobFlow.errors()) {
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
          chatThreadModel.sendText.start(text)
        }
      }
      transitionTo { state, _ ->
        if (state.inputValue.isBlank()) state else state.copy(inputValue = "")
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
