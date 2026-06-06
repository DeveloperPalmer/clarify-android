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
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.thread.domain.ThreadModel
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.entity.Approver
import ru.sla.clarify.feature.chat.thread.ui.entity.Commit
import ru.sla.clarify.feature.chat.thread.ui.mapper.toUiCommits
import ru.sla.clarify.feature.chat.thread.ui.routing.FlowEvent
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
      threadModel.subscribeOnCommitChanges(branchId)
      threadModel.markReadCommits(branchId)
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

    onEach(threadModel.user.filterNotNull()) {
      transitionTo { state, user ->
        state.copy(currentUserId = user.id)
      }
    }

    onEach(threadModel.branch(branchId).filterNotNull()) {
      transitionTo { state, branch ->
        state.copy(
          branchName = branch.name,
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
        // Защита: UI прячет инпут когда у ветки активный MR, но если что-то проскочит
        // из-за race condition — всё равно не постим commit в замороженную ветку.
        if (state.mergeRequest != null) return@action
        // state.commits отсортирован newest-first; индекс 0 — самое свежее сообщение.
        // Для первого сообщения в новой ветке это null, и ниже сгенерируется свежий цвет —
        // это намеренно, новая ветка получает свой цвет.
        val parentCommit = state.commits
          .filterIsInstance<Commit.Message>()
          .firstOrNull()
        threadModel.sendMessage.start(
          argument1 = branchId,
          argument2 = requireNotNull(text.trim().ifBlank { null }),
          argument3 = parentCommit?.source?.colorHex
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configurePeerCommitTransitions() {
    onEach(
      threadModel.commits(branchId)
        .map { it.toUiCommits() }
    ) {
      transitionTo { state, commits ->
        // SQL отдаёт ASC по timestamp; UI рендерит newest-first.
        state.copy(commits = commits.asReversed())
      }
    }

    onEach(intent(ViewIntents::markReadUpTo)) {
      action { _, _, lastReadAt ->
        threadModel.markReadUpTo(lastReadAt)
      }
    }

    onEach(threadModel.unreadCount) {
      transitionTo { state, unreadCount ->
        state.copy(unreadCount = unreadCount.toInt())
      }
    }
  }

  private fun MachineDsl<ViewState>.configureMergeRequestTransitions() {
    onEach(intent(ViewIntents::openMergeRequest)) {
      action { state, _, _ ->
        if (state.mergeRequest != null) return@action
        threadModel.openMergeRequest.start(branchId)
      }
    }
    onEach(intent(ViewIntents::approveMergeRequest)) {
      action { state, _, _ ->
        if (!state.mergeRequest.isPending()) return@action
        if (state.isCurrentUserApproved) return@action
        threadModel.approveMergeRequest.start(branchId)
      }
    }
    onEach(intent(ViewIntents::revokeApprovalMergeRequest)) {
      action { state, _, _ ->
        if (!state.mergeRequest.isPending()) return@action
        if (!state.isCurrentUserApproved) return@action
        threadModel.revokeApprovalMergeRequest.start(branchId)
      }
    }
    onEach(intent(ViewIntents::cancelMergeRequest)) {
      action { state, _, _ ->
        if (!state.mergeRequest.isPending()) return@action
        threadModel.cancelMergeRequest.start(branchId)
      }
    }
    onEach(intent(ViewIntents::finalizeMergeRequest)) {
      action { state, _, _ ->
        if (state.mergeRequest?.status != Branch.MergeRequest.Status.ReadyToMerge) return@action
        threadModel.finalizeMergeRequest.start(branchId)
      }
    }

    onEach(threadModel.mergeRequestInitiator(branchId)) {
      transitionTo { state, participant ->
        state.copy(initiatorName = participant?.displayName)
      }
    }

    onEach(
      combine(
        threadModel.participants,
        threadModel.branch(branchId)
      ) { participants, branch ->
        val approvedUids = branch?.mergeRequest?.approvedByIds.orEmpty()
        val approvers = participants.map { participant ->
          val userId = UserId(participant.id.value)
          Approver(
            userId = userId,
            displayName = participant.displayName,
            photoUrl = participant.photoUrl,
            isApproved = userId in approvedUids
          )
        }
        approvers
      }
    ) {
      transitionTo { state, approvers ->
        state.copy(approvers = approvers)
      }
    }

    onEach(
      combine(
        threadModel.openMergeRequest.jobFlow.state,
        threadModel.approveMergeRequest.jobFlow.state,
        threadModel.revokeApprovalMergeRequest.jobFlow.state,
        threadModel.cancelMergeRequest.jobFlow.state,
        threadModel.finalizeMergeRequest.jobFlow.state
      ) { states -> states.any { it == JobState.Running } }
    ) {
      transitionTo { state, pending ->
        state.copy(mergeRequestRunning = pending)
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

    onEach(threadModel.finalizeMergeRequest.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_finalize_failed)
      }
    }
  }

  /** Pending = MR открыт и ещё не зафинализирован (можно approve/revoke/cancel). */
  private fun Branch.MergeRequest?.isPending(): Boolean {
    return this?.status == Branch.MergeRequest.Status.Open ||
      this?.status == Branch.MergeRequest.Status.ReadyToMerge
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
