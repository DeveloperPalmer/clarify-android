package ru.sla.clarify.feature.chat.ui.screen.thread

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.screen.ViewModel
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
      chatModel.markReadTreadMessages()
      chatModel.loadHistory.startOnSubscribe()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ChatThreadDismissed)
      }
    }

    onEach(intent(ViewIntents::openChronology)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ChronologyRequested)
      }
    }

    configurePeerMessageTransitions()
    configureSenderMessageTransitions()
    configureHistoryMessagesTransitions()
  }

  private fun MachineDsl<ViewState>.configureSenderMessageTransitions() {
    onEach(intent(ViewIntents::sendMessage)) {
      action { _, _, text ->
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
          chatModel.sendText.start(trimmed)
        }
      }
    }

    onEach(
      chatModel.sendText.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(
          isSending = contentLoadState is ContentLoadState.Loading
        )
      }
    }

    onEach(chatModel.sendText.jobFlow.successResults()) {
      transitionTo { state, sent ->
        if (state.messages.any { it.id.value.isNotEmpty() && it.id == sent.id }) {
          return@transitionTo state
        }
        state.copy(
          messages = state.messages.addFirst(sent)
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configurePeerMessageTransitions() {
    onEach(chatModel.treadMessage(peerId)) {
      transitionTo { state, message ->
        if (state.messages.any { it.id.value.isNotEmpty() && it.id == message.id }) {
          return@transitionTo state
        }
        state.copy(
          messages = state.messages.addFirst(message)
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configureHistoryMessagesTransitions() {
    onEach(
      chatModel.loadHistory.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(
          contentLoadState = contentLoadState
        )
      }
    }

    onEach(chatModel.loadHistory.jobFlow.successResults()) {
      transitionTo { state, history ->
        state.copy(
          messages = ArrayDeque(
            history
              .sortedBy { it.timestamp }
              .asReversed()
          )
        )
      }
    }
  }
}

private fun <T> List<T>.addFirst(value: T): ArrayDeque<T> {
  return ArrayDeque(this).apply { addFirst(value) }
}
