package ru.sla.clarify.feature.chat.thread.ui.screen.thread

import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.thread.domain.ThreadModel
import ru.sla.clarify.feature.chat.thread.ui.routing.FlowEvent
import ru.sla.clarify.feature.entity.chat.Commit
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
      threadModel.fetchHistoryCommit.startOnSubscribe()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ThreadDismissed)
      }
    }

    configurePeerMessageTransitions()
    configureSenderMessageTransitions()
    configureHistoryMessagesTransitions()
    configureBranchTransitions()
  }

  private fun MachineDsl<ViewState>.configureBranchTransitions() {
    onEach(intent(ViewIntents::openBranch)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.BranchRequested)
      }
    }
  }

  private fun MachineDsl<ViewState>.configureSenderMessageTransitions() {
    onEach(intent(ViewIntents::sendCommit)) {
      action { state, _, text ->
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
          threadModel.sendMessage.start(
            argument1 = trimmed,
            argument2 = state.commits
              .filterIsInstance<Commit.Message>()
              .lastOrNull()
          )
        }
      }
    }

    onEach(
      threadModel.sendMessage.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(
          isSending = contentLoadState is ContentLoadState.Loading
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configurePeerMessageTransitions() {
    onEach(threadModel.commits) {
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
      threadModel.fetchHistoryCommit.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(
          contentLoadState = contentLoadState
        )
      }
    }
  }
}

private fun <T> List<T>.addFirst(value: T): ArrayDeque<T> {
  return ArrayDeque(this).apply { addFirst(value) }
}
