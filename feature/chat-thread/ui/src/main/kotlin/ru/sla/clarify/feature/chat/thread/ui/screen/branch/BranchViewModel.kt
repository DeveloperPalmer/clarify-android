package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterNotNull
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
import ru.sla.clarify.feature.chat.thread.domain.BranchRepository
import ru.sla.clarify.feature.chat.thread.domain.ThreadModel
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.routing.FlowEvent
import ru.sla.clarify.feature.entity.chat.Commit

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

    onEach(branchRepository.branch(branchId).filterNotNull()) {
      transitionTo { state, branch ->
        state.copy(
          branchName = branch.name,
          branchStatus = branch.status
        )
      }
    }

    configureSenderCommitTransitions()
    configurePeerCommitTransitions()
  }

  private fun MachineDsl<ViewState>.configureSenderCommitTransitions() {
    onEach(intent(ViewIntents::sendCommit)) {
      action { state, _, text ->
        if (state.branchStatus == Branch.Status.Active) {
          // state.commits is built via addFirst — index 0 is the newest commit.
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
}
