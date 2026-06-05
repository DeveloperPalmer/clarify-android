package ru.sla.clarify.feature.chat.thread.ui.screen.thread

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.thread.domain.ThreadModel
import ru.sla.clarify.feature.chat.thread.ui.entity.Commit
import ru.sla.clarify.feature.chat.thread.ui.mapper.toUiCommits
import ru.sla.clarify.feature.chat.thread.ui.routing.FlowEvent
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef
import javax.inject.Inject

class ThreadViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val threadModel: ThreadModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to {
      threadModel.markReadCommits()
      threadModel.fetchHistoryCommits.startOnSubscribe()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ThreadDismissed)
      }
    }

    onEach(threadModel.peer.filterNotNull()) {
      transitionTo { state, peer ->
        state.copy(peer = peer)
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

    onEach(
      threadModel.commits(branchId = null)
        .map { it.toUiCommits() }
    ) {
      transitionTo { state, commits ->
        state.copy(commits = commits.asReversed())
      }
    }

    onEach(intent(ViewIntents::markReadUpTo)) {
      action { _, _, lastReadAt ->
        threadModel.markReadUpTo(lastReadAt)
      }
    }

    onEach(threadModel.unreadCount()) {
      transitionTo { state, unreadCount ->
        state.copy(unreadCount = unreadCount.toInt())
      }
    }

    configureSendMessageTransitions()
    configureBranchTransitions()
  }

  private fun MachineDsl<ViewState>.configureSendMessageTransitions() {
    onEach(intent(ViewIntents::sendMessage)) {
      action { state, _, text ->
        // state.commits is built via addFirst — index 0 is the newest commit.
        val parentCommit = state.commits
          .filterIsInstance<Commit.Message>()
          .firstOrNull()
        threadModel.sendMessage.start(
          argument1 = null,
          argument2 = requireNotNull(text.trim().ifBlank { null }),
          argument3 = parentCommit?.source?.colorHex
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configureBranchTransitions() {
    onEach(threadModel.branches()) {
      transitionTo { state, branches ->
        state.copy(branches = branches)
      }
    }

    onEach(intent(ViewIntents::createBranch)) {
      action { _, _, commit ->
        sendViewEvent(showBranchCreationSheet(commit))
      }
    }

    onEach(intent(ViewIntents::showBranchesList)) {
      action { _, _, _ ->
        sendViewEvent(showBranchesListSheet())
      }
    }

    onEach(intent(ViewIntents::openBranch)) {
      action { _, _, branchId ->
        eventSink.sendEvent(FlowEvent.BranchRequested(branchId))
      }
    }

    onEach(intent(ViewIntents::confirmCreateBranch)) {
      action { _, _, createBranch ->
        threadModel.createBranch.start(
          argument1 = null,
          argument2 = createBranch.commit.source,
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
            message = resRef(R.string.thread_create_branch_creation_failed)
          )
        )
      }
    }
  }
}
