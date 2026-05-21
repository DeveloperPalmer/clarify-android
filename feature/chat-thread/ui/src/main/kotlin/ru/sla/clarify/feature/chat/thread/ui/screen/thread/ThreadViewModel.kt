package ru.sla.clarify.feature.chat.thread.ui.screen.thread

import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.errors
import ru.kode.remo.successResults
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
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.strRef
import javax.inject.Inject

class ThreadViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val threadModel: ThreadModel,
  private val peerId: Peer.Id
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState(peerId = peerId) to {
      threadModel.markReadCommits()
      threadModel.fetchHistoryCommits.startOnSubscribe()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ThreadDismissed)
      }
    }

    onEach(
      threadModel.fetchHistoryCommits.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(
          contentLoadState = contentLoadState
        )
      }
    }

    configurePeerCommitTransitions()
    configureSenderCommitTransitions()
    configureBranchTransitions()
  }

  private fun MachineDsl<ViewState>.configureBranchTransitions() {
    onEach(threadModel.branches()) {
      transitionTo { state, branches ->
        state.copy(branches = branches)
      }
    }

    onEach(intent(ViewIntents::showBranchSheet)) {
      action { _, _, commit ->
        sendViewEvent(showBranchCreationSheet(commit))
      }
    }

    onEach(intent(ViewIntents::showBranchesList)) {
      action { state, _, _ ->
        sendViewEvent(showBranchesListSheet(state.branches))
      }
    }

    onEach(intent(ViewIntents::openBranch)) {
      action { _, _, branchId ->
        eventSink.sendEvent(FlowEvent.BranchRequested(branchId))
      }
    }

    onEach(intent(ViewIntents::createBranch)) {
      action { _, _, createBranch ->
        threadModel.createBranch.start(
          argument1 = null,
          argument2 = createBranch.commit,
          argument3 = createBranch.name
        )
      }
    }

    onEach(threadModel.createBranch.jobFlow.successResults()) {
      action { state, _, branch ->
        eventSink.sendEvent(FlowEvent.BranchRequested(branch.id))
      }
    }

    onEach(threadModel.createBranch.jobFlow.errors()) {
      action { _, _, _ ->
        sendViewEvent(
          Snackbar(
            isError = true,
            message = strRef("Branch creation failed")
          )
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configureSenderCommitTransitions() {
    onEach(intent(ViewIntents::sendCommit)) {
      action { state, _, text ->
        // state.commits is built via addFirst — index 0 is the newest commit.
        val parentCommit = state.commits
          .filterIsInstance<Commit.Message>()
          .firstOrNull()
        threadModel.sendMessage.start(
          argument1 = null,
          argument2 = requireNotNull(text.trim().ifBlank { null }),
          argument3 = parentCommit?.colorHex
        )
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

  private fun MachineDsl<ViewState>.configurePeerCommitTransitions() {
    onEach(threadModel.commits(branchId = null)) {
      transitionTo { state, commits ->
        // SQL returns ASC by timestamp; UI renders newest-first.
        state.copy(commits = commits.asReversed())
      }
      action { _, _, _ ->
        threadModel.markReadCommits()
      }
    }
  }
}
