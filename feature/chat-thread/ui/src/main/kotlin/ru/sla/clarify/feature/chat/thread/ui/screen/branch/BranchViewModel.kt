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
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.thread.domain.ThreadModel
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.routing.FlowEvent
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef

class BranchViewModel @AssistedInject constructor(
  private val eventSink: FlowEventSink,
  private val threadModel: ThreadModel,
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
      // нечего делать
    }

    onEach(threadModel.userId()) {
      transitionTo { state, userId ->
        state.copy(currentUserId = userId)
      }
    }

    onEach(threadModel.branch(branchId).filterNotNull()) {
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
    configureMergeRequestTransitions()
  }

  private fun MachineDsl<ViewState>.configureSenderCommitTransitions() {
    onEach(intent(ViewIntents::sendCommit)) {
      action { state, _, text ->
        // Защита: UI прячет инпут когда status != Active, но если что-то проскочит из-за
        // race condition — всё равно не постим commit в замороженную ветку.
        if (state.branchStatus != Branch.Status.Active) return@action
        // state.commits отсортирован newest-first; индекс 0 — самое свежее сообщение.
        // Для первого сообщения в новой ветке это null, и ниже сгенерируется свежий цвет —
        // это намеренно, новая ветка получает свой цвет.
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
        // SQL отдаёт ASC по timestamp; UI рендерит newest-first.
        state.copy(commits = commits.asReversed())
      }
      action { _, _, _ ->
        threadModel.markReadCommits()
      }
    }
  }

  private fun MachineDsl<ViewState>.configureMergeRequestTransitions() {
    onEach(intent(ViewIntents::requestMerge)) {
      action { state, _, _ ->
        if (state.branchStatus != Branch.Status.Active) return@action
        threadModel.openMergeRequest.start(branchId)
      }
    }
    onEach(intent(ViewIntents::approveMerge)) {
      action { state, _, _ ->
        if (state.branchStatus != Branch.Status.MergeInProgress) return@action
        if (state.isCurrentUserApprover) return@action
        threadModel.approveMergeRequest.start(branchId)
      }
    }
    onEach(intent(ViewIntents::revokeApproval)) {
      action { state, _, _ ->
        if (state.branchStatus != Branch.Status.MergeInProgress) return@action
        threadModel.revokeApprovalMergeRequest.start(branchId)
      }
    }

    onEach(threadModel.mergeRequestInitiator(branchId)) {
      transitionTo { state, participant ->
        state.copy(initiatorName = participant?.displayName)
      }
    }

    // Агрегируем isMergeActionPending по всем четырём merge-задачам. Если хоть одна
    // в Running -> UI показывает прогресс и блокирует кнопки.
    onEach(
      combine(
        threadModel.openMergeRequest.jobFlow.state,
        threadModel.approveMergeRequest.jobFlow.state,
        threadModel.revokeApprovalMergeRequest.jobFlow.state,
        threadModel.cancelMergeRequest.jobFlow.state
      ) { a, b, c, d ->
        listOf(a, b, c, d).any { it == JobState.Running }
      }
    ) {
      transitionTo { state, pending ->
        state.copy(isMergeActionPending = pending)
      }
    }

    // Ошибки показываем снэкбарами; recovery — пользователь повторяет вручную.
    onEach(threadModel.openMergeRequest.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_request_failed)
      }
    }

    onEach(threadModel.approveMergeRequest.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_approve_failed)
      }
    }

    onEach(threadModel.revokeApprovalMergeRequest.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_revoke_failed)
      }
    }

    onEach(threadModel.cancelMergeRequest.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_cancel_failed)
      }
    }
  }

  private fun showMergeError(messageId: Int) {
    sendViewEvent(
      Snackbar(
        isError = true,
        message = resRef(messageId)
      )
    )
  }
}
