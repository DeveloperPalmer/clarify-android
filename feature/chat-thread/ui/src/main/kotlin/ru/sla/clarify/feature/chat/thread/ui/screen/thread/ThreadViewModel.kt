package ru.sla.clarify.feature.chat.thread.ui.screen.thread

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
import ru.sla.clarify.feature.chat.thread.domain.ThreadModel
import ru.sla.clarify.feature.chat.thread.ui.routing.FlowEvent
import ru.sla.clarify.feature.entity.chat.Peer
import javax.inject.Inject

class ThreadViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val threadModel: ThreadModel,
  private val peerId: Peer.Id
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState(peerId = peerId) to {
      threadModel.markReadCommits()
      threadModel.getCommitHistory.startOnSubscribe()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ThreadDismissed)
      }
    }

    configurePeerMessageTransitions()
    configureSenderMessageTransitions()
    configureHistoryMessagesTransitions()
  }

  private fun MachineDsl<ViewState>.configureSenderMessageTransitions() {
    onEach(intent(ViewIntents::sendCommit)) {
      action { state, _, text ->
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
          threadModel.sendCommit.start(trimmed)
        }
      }
    }

    onEach(
      threadModel.sendCommit.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(
          isSending = contentLoadState is ContentLoadState.Loading
        )
      }
    }

    onEach(threadModel.sendCommit.jobFlow.successResults()) {
      transitionTo { state, sent ->
        if (state.commits.any { it.id.value.isNotEmpty() && it.id == sent.id }) {
          return@transitionTo state
        }
        state.copy(
          commits = state.commits.addFirst(sent)
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configurePeerMessageTransitions() {
    onEach(threadModel.peerCommits()) {
      transitionTo { state, message ->
        if (state.commits.any { it.id.value.isNotEmpty() && it.id == message.id }) {
          return@transitionTo state
        }
        state.copy(
          commits = state.commits.addFirst(message)
        )
      }
      action { _, _, _ ->
        threadModel.markReadCommits()
      }
    }
  }

  private fun MachineDsl<ViewState>.configureHistoryMessagesTransitions() {
    onEach(
      threadModel.getCommitHistory.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(
          contentLoadState = contentLoadState
        )
      }
    }

    onEach(threadModel.getCommitHistory.jobFlow.successResults()) {
      transitionTo { state, history ->
        state.copy(
          commits = ArrayDeque(
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
