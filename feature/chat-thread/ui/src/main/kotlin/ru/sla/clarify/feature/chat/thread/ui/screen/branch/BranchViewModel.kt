package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.JobState
import ru.kode.remo.errors
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.thread.domain.BranchRepository
import ru.sla.clarify.feature.chat.thread.domain.ThreadModel
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.routing.FlowEvent
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.strRef

class BranchViewModel @AssistedInject constructor(
  private val eventSink: FlowEventSink,
  private val threadModel: ThreadModel,
  private val branchRepository: BranchRepository,
  @Assisted
  branchIdValue: String
) : ViewModel<ViewState, ViewIntents>() {

  private val branchId: Branch.Id = Branch.Id(branchIdValue)

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState(branchId = branchId) to {
      threadModel.markReadCommits()
      threadModel.fetchHistoryBranchCommits.startOnSubscribe(branchId)
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.BranchDismissed)
      }
    }

    onEach(
      threadModel.fetchHistoryBranchCommits.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(
          contentLoadState = contentLoadState
        )
      }
    }

    onEach(threadModel.subscribeOnCommits(branchId)) {
      // nothing to do
    }

    onEach(threadModel.userId()) {
      transitionTo { state, userId ->
        state.copy(currentUserId = userId)
      }
    }

    onEach(branchRepository.branch(branchId).filterNotNull()) {
      transitionTo { state, branch ->
        state.copy(
          branchName = branch.name,
          branchStatus = branch.status,
          mergeRequest = branch.mergeRequest
        )
      }
    }

    configureSenderCommitTransitions()
    configurePeerCommitTransitions()
    configureMergeTransitions()
  }

  private fun MachineDsl<ViewState>.configureSenderCommitTransitions() {
    onEach(intent(ViewIntents::sendCommit)) {
      action { state, _, text ->
        // Defensive: input is hidden by the UI when status != Active, but if anything
        // racy slipped through we still don't post a commit into a frozen branch.
        if (state.branchStatus != Branch.Status.Active) return@action
        // state.commits is sorted newest-first; index 0 is the freshest message.
        // For the first message in a brand-new branch this is null, so a fresh color
        // will be generated downstream — that's intentional, a new branch gets its own color.
        val parentCommit = state.commits
          .filterIsInstance<Commit.Message>()
          .firstOrNull()
        threadModel.sendMessage.start(
          argument1 = branchId,
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
    onEach(threadModel.commits(branchId)) {
      transitionTo { state, commits ->
        // SQL returns ASC by timestamp; UI renders newest-first.
        state.copy(commits = commits.asReversed())
      }
      action { _, _, _ ->
        threadModel.markReadCommits()
      }
    }
  }

  private fun MachineDsl<ViewState>.configureMergeTransitions() {
    onEach(intent(ViewIntents::requestMerge)) {
      action { state, _, _ ->
        if (state.branchStatus != Branch.Status.Active) return@action
        threadModel.requestMerge.start(branchId)
      }
    }
    onEach(intent(ViewIntents::approveMerge)) {
      action { state, _, _ ->
        if (state.branchStatus != Branch.Status.MergeInProgress) return@action
        if (state.isCurrentUserApprover) return@action
        threadModel.approveMerge.start(branchId)
      }
    }
    onEach(intent(ViewIntents::revokeApproval)) {
      action { state, _, _ ->
        if (state.branchStatus != Branch.Status.MergeInProgress) return@action
        threadModel.revokeApproval.start(branchId)
      }
    }

    // Aggregate isMergeActionPending across all four merge-related tasks. Any one of them
    // being in Running state -> UI shows progress + disables buttons.
    onEach(
      combine(
        threadModel.requestMerge.jobFlow.state,
        threadModel.approveMerge.jobFlow.state,
        threadModel.revokeApproval.jobFlow.state,
        threadModel.cancelMergeRequest.jobFlow.state
      ) { a, b, c, d ->
        listOf(a, b, c, d).any { it == JobState.Running }
      }
    ) {
      transitionTo { state, pending ->
        state.copy(isMergeActionPending = pending)
      }
    }

    // Surface failures as snackbars; recovery is just "user tries again".
    onEach(threadModel.requestMerge.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError("Failed to request merge")
      }
    }

    onEach(threadModel.approveMerge.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError("Failed to approve merge")
      }
    }

    onEach(threadModel.revokeApproval.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError("Failed to revoke approval")
      }
    }

    onEach(threadModel.cancelMergeRequest.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError("Failed to cancel merge request")
      }
    }
  }

  private fun showMergeError(text: String) {
    sendViewEvent(
      Snackbar(
        isError = true,
        message = strRef(text)
      )
    )
  }
}
