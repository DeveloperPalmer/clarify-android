package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
import ru.sla.clarify.feature.chat.direct.thread.ui.mapper.withSelection
import ru.sla.clarify.feature.chat.direct.thread.ui.routing.FlowEvent
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef
import javax.inject.Inject
import ru.sla.clarify.entity.chat.Commit as DomainCommit

class ThreadViewModel(
  private val eventSink: FlowEventSink,
  private val directThreadModel: DirectThreadModel,
  dispatcher: CoroutineDispatcher
) : ViewModel<ViewState, ViewIntents>(dispatcher) {

  @Inject
  constructor(
    eventSink: FlowEventSink,
    directThreadModel: DirectThreadModel
  ) : this(eventSink, directThreadModel, Dispatchers.Default)

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
      transitionTo { state, uiCommits ->
        val presentIds = uiCommits.mapTo(mutableSetOf()) { it.source.id }
        val selectedCommitIds = state.selectedCommitIds.filter { it in presentIds }
        val menuCommit = state.menuCommit?.takeIf { it.source.id in presentIds }
        state.applySelection(
          commits = uiCommits,
          selectedCommitIds = selectedCommitIds
        ).copy(menuCommit = menuCommit)
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
    configureSelectionTransitions()
    configureCommitMenuTransitions()
    configureCommitDeletionTransitions()
  }

  private fun MachineDsl<ViewState>.configureCommitMenuTransitions() {
    onEach(intent(ViewIntents::showCommitMenu)) {
      transitionTo { state, commit ->
        state.copy(menuCommit = commit)
      }
    }

    onEach(intent(ViewIntents::dismissCommitMenu)) {
      transitionTo { state, _ ->
        state.copy(menuCommit = null)
      }
    }
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

  private fun MachineDsl<ViewState>.configureSelectionTransitions() {
    onEach(intent(ViewIntents::toggleMessageSelection)) {
      transitionTo { state, commit ->
        val commitId = commit.source.id
        val selectedCommitIds = if (commitId in state.selectedCommitIds) {
          state.selectedCommitIds - commitId
        } else {
          state.selectedCommitIds + commitId
        }
        state.applySelection(selectedCommitIds)
      }
    }

    onEach(intent(ViewIntents::clearSelection)) {
      transitionTo { state, _ ->
        state.applySelection(emptyList())
      }
    }
  }

  private fun MachineDsl<ViewState>.configureCommitDeletionTransitions() {
    onEach(intent(ViewIntents::deleteCommit)) {
      action { state, _, _ ->
        val viewEvent = showDeleteMessagesDialog(
          count = state.selectedCommitIds.size,
          peerName = state.peer?.displayName.orEmpty()
        )
        sendViewEvent(viewEvent)
      }
    }

    onEach(intent(ViewIntents::confirmDeleteCommit)) {
      action { state, _, forEveryone ->
        directThreadModel.deleteCommits.start(state.selectedCommitIds, forEveryone)
      }
    }

    onEach(directThreadModel.deleteCommits.jobFlow.successResults()) {
      transitionTo { state, _ ->
        state.applySelection(emptyList())
      }
    }

    onEach(directThreadModel.deleteCommits.jobFlow.errors()) {
      action { _, _, _ ->
        sendViewEvent(
          Snackbar(
            isError = true,
            message = resRef(R.string.thread_delete_failed)
          )
        )
      }
    }
  }
}

private fun ViewState.applySelection(
  selectedCommitIds: List<DomainCommit.Id>,
  commits: List<Commit> = this.commits
): ViewState {
  val selectionMode = selectedCommitIds.isNotEmpty()
  return copy(
    selectionMode = selectionMode,
    selectedCommitIds = selectedCommitIds,
    commits = commits.withSelection(selectionMode, selectedCommitIds)
  )
}
