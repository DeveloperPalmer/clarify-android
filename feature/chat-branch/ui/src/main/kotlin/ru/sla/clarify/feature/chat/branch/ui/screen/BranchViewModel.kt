package ru.sla.clarify.feature.chat.branch.ui.screen

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.domain.entity.EditedMessage
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Branch.MergeRequest.Status
import ru.sla.clarify.feature.chat.branch.domain.BranchModel
import ru.sla.clarify.feature.chat.branch.domain.entity.EditTargetNotFoundException
import ru.sla.clarify.feature.chat.branch.domain.entity.TargetParams
import ru.sla.clarify.feature.chat.branch.ui.entity.Approver
import ru.sla.clarify.feature.chat.branch.ui.routing.FlowEvent
import ru.sla.clarify.mapper.ui.toUiCommits
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.uikit.component.chat.Textual
import ru.sla.clarify.uikit.component.chat.message
import ru.sla.clarify.uikit.component.chat.selected
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef
import ru.sla.clarify.entity.chat.Commit as DomainCommit

class BranchViewModel(
  private val params: TargetParams,
  private val eventSink: FlowEventSink,
  private val branchModel: BranchModel,
  dispatcher: CoroutineDispatcher
) : ViewModel<ViewState, ViewIntents>(dispatcher) {

  @Inject
  constructor(
    params: TargetParams,
    eventSink: FlowEventSink,
    branchModel: BranchModel
  ) : this(params, eventSink, branchModel, Dispatchers.Default)

  override fun buildMachine(): Machine<ViewState> = machine {
    initial = ViewState(branchId = params.branchId) to {
      branchModel.markReadCommits()
      branchModel.fetchLatestCommits.startOnSubscribe()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.BranchDismissed)
      }
    }

    onEach(
      branchModel.fetchLatestCommits.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
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

    // Имя автора цитаты резолвится локально по участникам ветки, поэтому лента собирается
    // вместе с ними: свои цитаты подписываются «Вы», чужие — именем участника.
    onEach(
      combine(
        branchModel.commits,
        branchModel.members
      ) { commits, members ->
        commits.toUiCommits(
          memberNames = members.associate { UserId(it.id.value) to it.displayName }
        )
      }
    ) {
      transitionTo { state, commits ->
        val commitsIds = commits.mapTo(mutableSetOf()) { it.source.id }
        val selectedCommitIds = state.selectedCommitIds.filter { it in commitsIds }
        val menuCommit = state.focusedCommit?.takeIf { it.source.id in commitsIds }
        val editingMessage = state.editingCommit?.takeIf { it.source.id in commitsIds }
        val replyingMessage = state.replyingCommit?.takeIf { it.source.id in commitsIds }
        state.copy(
          focusedCommit = menuCommit,
          editingCommit = editingMessage,
          replyingCommit = replyingMessage
        ).updateSelection(
          commits = commits,
          selectedCommitIds = selectedCommitIds
        )
      }
      action { prevState, newState, _ ->
        // Цель режима исчезла из ленты (удалили здесь или на другом устройстве) — режим уже
        // сброшен транзишеном выше, осталось объяснить это пользователю.
        if (prevState.editingCommit != null && newState.editingCommit == null) {
          sendViewEvent(Snackbar(message = resRef(R.string.thread_edit_target_deleted)))
        }
        if (prevState.replyingCommit != null && newState.replyingCommit == null) {
          sendViewEvent(Snackbar(message = resRef(R.string.thread_reply_target_deleted)))
        }
      }
    }

    onEach(intent(ViewIntents::markMessageAsRead)) {
      action { _, _, lastReadAt ->
        branchModel.markReadUpTo(lastReadAt)
      }
    }

    onEach(branchModel.unreadCount) {
      transitionTo { state, unreadCount ->
        state.copy(unreadCount = unreadCount.toInt())
      }
    }

    configureCommitHistoryTransitions()
    configureCommitMessageTransitions()
    configureCommitReplyTransitions()
    configureCommitEditTransitions()
    configureCommitDeletionTransitions()
    configureCommitMenuTransitions()
    configureMergeRequestTransitions()
    configureSelectionTransitions()
  }

  private fun MachineDsl<ViewState>.configureCommitHistoryTransitions() {
    onEach(branchModel.hasCommitsHistory) {
      transitionTo { state, hasCommitsHistory ->
        state.copy(canLoadCommitsHistory = hasCommitsHistory)
      }
    }

    onEach(
      branchModel.fetchCommitHistory.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(loadingCommitsHistory = contentLoadState is ContentLoadState.Loading)
      }
    }

    onEach(branchModel.fetchCommitHistory.jobFlow.errors()) {
      action { _, _, _ ->
        val viewEvent = Snackbar(
          isError = true,
          message = resRef(R.string.thread_load_history_failed)
        )
        sendViewEvent(viewEvent)
      }
    }

    onEach(intent(ViewIntents::loadCommitsHistory)) {
      action { _, _, _ ->
        branchModel.fetchCommitHistory.start(
          queueingStrategy = QueueingStrategy.SkipNew
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configureCommitMessageTransitions() {
    onEach(intent(ViewIntents::copyMessage)) {
      action { _, _, _ ->
        sendViewEvent(
          Snackbar(message = resRef(R.string.thread_message_copied))
        )
      }
    }

    onEach(intent(ViewIntents::sendMessage)) {
      action { state, _, text ->
        if (state.mergeRequest != null) {
          return@action
        }
        branchModel.sendMessage(
          text = text.trim(),
          replyCommit = null
        )
      }
    }

    onEach(intent(ViewIntents::replyMessage)) {
      // Режим ответа снимается сразу: композер возвращается в обычный вид, не дожидаясь сервера
      // (как и при обычной отправке).
      transitionTo { state, _ ->
        state.copy(replyingCommit = null)
      }
      action { state, _, text ->
        if (state.mergeRequest != null) {
          return@action
        }
        branchModel.sendMessage(
          text = text.trim(),
          replyCommit = state.replyingCommit?.source as? DomainCommit.Message
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configureCommitReplyTransitions() {
    onEach(intent(ViewIntents::showReplyMessage)) {
      transitionTo { state, commit ->
        // Ответ и редактирование взаимоисключающие: вход в один режим снимает другой.
        state.copy(
          replyingCommit = commit,
          editingCommit = null,
          focusedCommit = null
        )
      }
    }

    onEach(intent(ViewIntents::hideReplyMessage)) {
      transitionTo { state, _ ->
        state.copy(replyingCommit = null)
      }
    }

    onEach(intent(ViewIntents::showQuotedMessage)) {
      transitionTo { state, commitId ->
        state.copy(highlightedCommitId = commitId)
      }
    }

    onEach(intent(ViewIntents::clearHighlightedCommit)) {
      transitionTo { state, _ ->
        state.copy(highlightedCommitId = null)
      }
    }
  }

  private fun MachineDsl<ViewState>.configureCommitEditTransitions() {
    onEach(intent(ViewIntents::showEditMessage)) {
      transitionTo { state, commit ->
        state.copy(
          editingCommit = commit.takeIf { it.source.isSelf },
          replyingCommit = null,
          focusedCommit = null
        )
      }
    }

    onEach(intent(ViewIntents::hideEditMessage)) {
      transitionTo { state, _ ->
        state.copy(editingCommit = null)
      }
    }

    onEach(intent(ViewIntents::confirmEditMessage)) {
      transitionTo { state, text ->
        val editingMessage = state.editingCommit
        // UI гасит кнопку для пустого и неизменённого текста; здесь тот же гейт на случай гонки.
        // Оптимистично закрываем композер сразу: правка уходит в фон, а сообщение уже показывает
        // новый текст с «часами». Кэш и статус ведёт репозиторий.
        if (editingMessage != null && EditedMessage.validate(text, (editingMessage as? Textual)?.text).isRight()) {
          state.copy(editingCommit = null)
        } else {
          state
        }
      }
      action { state, _, text ->
        // state здесь — прошлое состояние (ещё с editingCommit), transitionTo его уже обнулил.
        val editingMessage = state.editingCommit ?: return@action
        EditedMessage.validate(
          text = text,
          original = (editingMessage as? Textual)?.text
        ).onRight { edited ->
          branchModel.editCommit.start(
            argument1 = editingMessage.source.id,
            argument2 = edited.value
          )
        }
      }
    }

    onEach(branchModel.editCommit.jobFlow.errors()) {
      action { _, _, error ->
        // Композер уже закрыт (оптимистично), поэтому «цель удалена» показываем отсюда, а не через
        // transitionTo списка коммитов. Прочие ошибки репозиторий откатил — сообщаем красным.
        val event = if (error is EditTargetNotFoundException) {
          Snackbar(message = resRef(R.string.thread_edit_target_deleted))
        } else {
          Snackbar(isError = true, message = resRef(R.string.thread_edit_failed))
        }
        sendViewEvent(event)
      }
    }
  }

  private fun MachineDsl<ViewState>.configureCommitDeletionTransitions() {
    onEach(intent(ViewIntents::deleteCommit)) {
      action { _, _, commit ->
        sendViewEvent(showDeleteMessagesDialog(commit.source.id))
      }
    }

    onEach(intent(ViewIntents::deleteCommits)) {
      action { _, _, _ ->
        sendViewEvent(showDeleteMessagesDialog(null))
      }
    }

    onEach(intent(ViewIntents::confirmDeleteCommits)) {
      transitionTo { state, _ ->
        // Оптимистично выходим из режима выделения сразу: удаление применяется к кэшу и уходит
        // в фон, лента не ждёт ответа сервера. При ошибке репозиторий вернёт сообщения на место.
        state.updateSelection(selectedCommitIds = emptyList())
      }
      action { _, _, deleteCommits ->
        branchModel.deleteCommits.start(
          deleteCommits.ids,
          deleteCommits.forEveryone
        )
      }
    }

    onEach(branchModel.deleteCommits.jobFlow.errors()) {
      action { _, _, _ ->
        val viewEvent = Snackbar(
          isError = true,
          message = resRef(R.string.thread_delete_failed)
        )
        sendViewEvent(viewEvent)
      }
    }
  }

  private fun MachineDsl<ViewState>.configureCommitMenuTransitions() {
    onEach(intent(ViewIntents::showMessageMenu)) {
      transitionTo { state, commit ->
        state.copy(focusedCommit = commit)
      }
    }

    onEach(intent(ViewIntents::hideMessageMenu)) {
      transitionTo { state, _ ->
        state.copy(focusedCommit = null)
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

    onEach(branchModel.finalizeMergeRequest.jobFlow.errors()) {
      action { _, _, _ ->
        showMergeError(R.string.branch_merge_finalize_failed)
      }
    }
  }

  private fun MachineDsl<ViewState>.configureSelectionTransitions() {
    onEach(intent(ViewIntents::disableSelectionMode)) {
      transitionTo { state, _ ->
        state.updateSelection(
          selectedCommitIds = emptyList()
        )
      }
    }

    onEach(intent(ViewIntents::toggleSelectionMode)) {
      transitionTo { state, commit ->
        val targetCommitId = commit.source.id
        val updatedCommitIds = if (targetCommitId in state.selectedCommitIds) {
          state.selectedCommitIds - targetCommitId
        } else {
          state.selectedCommitIds + targetCommitId
        }
        state.updateSelection(
          selectedCommitIds = updatedCommitIds
        )
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

private fun ViewState.updateSelection(
  commits: List<Commit> = this.commits,
  selectedCommitIds: List<DomainCommit.Id>
): ViewState {
  val selectedIds = selectedCommitIds.toSet()
  return copy(
    selectionEnabled = selectedIds.isNotEmpty(),
    selectedCommitIds = selectedCommitIds,
    commits = commits.map { Commit.message.selected.set(it, it.source.id in selectedIds) }
  )
}
