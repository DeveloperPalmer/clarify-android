package ru.sla.clarify.feature.chat.branch.ui.screen

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.JobState
import ru.kode.remo.QueueingStrategy
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Branch.MergeRequest.Status
import ru.sla.clarify.feature.chat.branch.domain.BranchModel
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams
import ru.sla.clarify.feature.chat.branch.ui.entity.Approver
import ru.sla.clarify.feature.chat.branch.ui.routing.FlowEvent
import ru.sla.clarify.mapper.ui.toUiCommits
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef

class BranchViewModel @Inject constructor(
  private val params: TargetParams,
  private val eventSink: FlowEventSink,
  private val branchModel: BranchModel
) : ViewModel<ViewState, ViewIntents>() {

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState(branchId = params.branchId) to {
      branchModel.markReadCommits()
      branchModel.fetchHistoryCommits.startOnSubscribe()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.BranchDismissed)
      }
    }

    onEach(
      branchModel.fetchHistoryCommits.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(
          contentLoadState = contentLoadState
        )
      }
    }

    onEach(branchModel.user.filterNotNull()) {
      transitionTo { state, user ->
        state.copy(currentUserId = user.id)
      }
    }

    onEach(branchModel.members) {
      transitionTo { state, members ->
        state.copy(members = members)
      }
    }

    onEach(branchModel.branch.filterNotNull()) {
      transitionTo { state, branch ->
        state.copy(
          branchName = branch.name,
          mergeRequest = branch.mergeRequest,
          mergeRequestVisible = state.mergeRequestVisible
            .takeIf { branch.mergeRequest != null && branch.mergeRequest?.status != Status.Merged }
            ?: false
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
        if (state.mergeRequest != null) {
          return@action
        }
        branchModel.sendMessage.start(
          argument = text.trim().ifBlank { null }
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configurePeerCommitTransitions() {
    onEach(branchModel.commits.map { it.toUiCommits() }) {
      transitionTo { state, commits ->
        state.copy(commits = commits)
      }
    }

    onEach(intent(ViewIntents::markReadUpTo)) {
      action { _, _, lastReadAt ->
        branchModel.markReadUpTo(lastReadAt)
      }
    }

    onEach(branchModel.unreadCount) {
      transitionTo { state, unreadCount ->
        state.copy(unreadCount = unreadCount.toInt())
      }
    }
  }

  private fun MachineDsl<ViewState>.configureMergeRequestTransitions() {
    onEach(intent(ViewIntents::openMergeRequest)) {
      transitionTo { state, _ ->
        state.copy(mergeRequestVisible = true)
      }
      action { state, _, _ ->
        if (state.mergeRequest == null) {
          branchModel.openMergeRequest.start(
            queueingStrategy = QueueingStrategy.SkipNew
          )
        }
      }
    }

    onEach(intent(ViewIntents::hideMergeRequest)) {
      transitionTo { state, _ ->
        state.copy(mergeRequestVisible = false)
      }
    }

    onEach(intent(ViewIntents::approveMergeRequest)) {
      action { state, _, _ ->
        if (!state.mergeRequest.isPending()) return@action
        if (state.isCurrentUserApproved) return@action
        branchModel.approveMergeRequest.start(
          queueingStrategy = QueueingStrategy.SkipNew
        )
      }
    }

    onEach(intent(ViewIntents::revokeApprovalMergeRequest)) {
      action { state, _, _ ->
        if (!state.mergeRequest.isPending()) return@action
        if (!state.isCurrentUserApproved) return@action
        branchModel.revokeApprovalMergeRequest.start(
          queueingStrategy = QueueingStrategy.SkipNew
        )
      }
    }

    onEach(intent(ViewIntents::cancelMergeRequest)) {
      action { state, _, _ ->
        if (!state.mergeRequest.isPending()) return@action
        branchModel.cancelMergeRequest.start(
          queueingStrategy = QueueingStrategy.SkipNew
        )
      }
    }

    onEach(intent(ViewIntents::finalizeMergeRequest)) {
      action { state, _, _ ->
        if (state.mergeRequest?.status != Status.ReadyToMerge) return@action
        branchModel.finalizeMergeRequest.start(
          queueingStrategy = QueueingStrategy.SkipNew
        )
      }
    }

    onEach(branchModel.mergeRequestInitiator) {
      transitionTo { state, member ->
        state.copy(initiatorName = member?.displayName)
      }
    }

    onEach(
      combine(
        branchModel.members,
        branchModel.branch
      ) { members, branch ->
        val approvedUids = branch?.mergeRequest?.approvedByIds.orEmpty()
        val approvers = members.map { member ->
          val userId = UserId(member.id.value)
          Approver(
            userId = userId,
            displayName = member.displayName,
            photoUrl = member.photoUrl,
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
        branchModel.openMergeRequest.jobFlow.state,
        branchModel.approveMergeRequest.jobFlow.state,
        branchModel.revokeApprovalMergeRequest.jobFlow.state,
        branchModel.cancelMergeRequest.jobFlow.state,
        branchModel.finalizeMergeRequest.jobFlow.state
      ) { states -> states.any { it == JobState.Running } }
    ) {
      transitionTo { state, pending ->
        state.copy(mergeRequestInProgress = pending)
      }
    }

    onEach(branchModel.openMergeRequest.jobFlow.errors()) {
      transitionTo { state, _ ->
        state.copy(mergeRequestVisible = false)
      }
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_request_failed)
      }
    }

    onEach(branchModel.approveMergeRequest.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_approve_failed)
      }
    }

    onEach(branchModel.revokeApprovalMergeRequest.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_revoke_failed)
      }
    }

    onEach(branchModel.cancelMergeRequest.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_cancel_failed)
      }
    }

    onEach(branchModel.finalizeMergeRequest.jobFlow.successResults()) {
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_finalize_failed)
      }
    }

    onEach(branchModel.finalizeMergeRequest.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_finalize_failed)
      }
    }
  }

  /** Pending = MR открыт и ещё не зафинализирован (можно approve/revoke/cancel). */
  private fun Branch.MergeRequest?.isPending(): Boolean {
    return this?.status == Status.Open ||
      this?.status == Status.ReadyToMerge
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
