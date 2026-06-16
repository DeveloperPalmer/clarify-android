package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

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
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadModel
import ru.sla.clarify.feature.chat.direct.thread.ui.mapper.toUiCommits
import ru.sla.clarify.feature.chat.direct.thread.ui.routing.FlowEvent
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef
import javax.inject.Inject

class ThreadViewModel @Inject constructor(
  private val eventSink: FlowEventSink,
  private val directThreadModel: DirectThreadModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState() to {
      directThreadModel.markReadCommits()
      directThreadModel.fetchHistoryCommits.startOnSubscribe()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.ThreadDismissed)
      }
    }

    onEach(directThreadModel.peer.filterNotNull()) {
      transitionTo { state, peer ->
        state.copy(peer = peer)
      }
    }

    onEach(
      directThreadModel.fetchHistoryCommits.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }

    onEach(directThreadModel.commits.map { it.toUiCommits() }) {
      transitionTo { state, commits ->
        state.copy(commits = commits.asReversed())
      }
    }

    onEach(intent(ViewIntents::markReadUpTo)) {
      action { _, _, lastReadAt ->
        directThreadModel.markReadUpTo(lastReadAt)
      }
    }

    onEach(directThreadModel.unreadCount) {
      transitionTo { state, unreadCount ->
        state.copy(unreadCount = unreadCount.toInt())
      }
    }

    configureSendMessageTransitions()
    configureBranchTransitions()
  }

  private fun MachineDsl<ViewState>.configureSendMessageTransitions() {
    onEach(intent(ViewIntents::sendMessage)) {
      action { _, _, text ->
        directThreadModel.sendMessage.start(
          requireNotNull(text.trim().ifBlank { null })
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configureBranchTransitions() {
    onEach(directThreadModel.branches) {
      transitionTo { state, branches ->
        state.copy(branches = branches)
      }
    }

    onEach(intent(ViewIntents::createBranch)) {
      action { _, _, commit ->
        sendViewEvent(showBranchCreationModalSheet(commit))
      }
    }

    onEach(intent(ViewIntents::showBranchesList)) {
      action { _, _, _ ->
        sendViewEvent(showBranchesModalSheet())
      }
    }

    onEach(intent(ViewIntents::openBranch)) {
      action { _, _, branchId ->
        eventSink.sendEvent(FlowEvent.BranchRequested(branchId))
      }
    }

    onEach(intent(ViewIntents::confirmCreateBranch)) {
      action { _, _, createBranch ->
        directThreadModel.createBranch.start(
          argument1 = null,
          argument2 = createBranch.commit.source,
          argument3 = createBranch.name
        )
      }
    }

    onEach(intent(ViewIntents::showCreateBranchError)) {
      transitionTo { state, createBranchError ->
        state.copy(createBranchError = createBranchError)
      }
    }

    onEach(intent(ViewIntents::clearCreateBranchError)) {
      transitionTo { state, _ ->
        state.copy(createBranchError = null)
      }
    }

    onEach(directThreadModel.createBranch.jobFlow.successResults()) {
      action { state, _, branchId ->
        eventSink.sendEvent(FlowEvent.BranchRequested(branchId))
      }
    }

    onEach(directThreadModel.createBranch.jobFlow.errors()) {
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
