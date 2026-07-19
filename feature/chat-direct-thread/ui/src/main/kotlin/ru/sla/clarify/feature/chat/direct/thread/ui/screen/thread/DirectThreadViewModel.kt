package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadModel
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.EditTargetNotFoundException
import ru.sla.clarify.feature.chat.direct.thread.ui.routing.FlowEvent
import ru.sla.clarify.mapper.ui.toUiCommits
import ru.sla.clarify.uikit.component.bubble.isSelected
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.uikit.component.chat.bubble
import ru.sla.clarify.uikit.component.chat.message
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef
import ru.sla.clarify.entity.chat.Commit as DomainCommit

class DirectThreadViewModel(
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
      directThreadModel.configureDirectThread.startOnSubscribe()
    }

    onEach(intent(ViewIntents::navigateBack)) {
      action { _, _, _ ->
        eventSink.sendEvent(FlowEvent.DirectThreadDismissed)
      }
    }

    onEach(directThreadModel.peer.filterNotNull()) {
      transitionTo { state, peer ->
        state.copy(peer = peer)
      }
    }

    onEach(
      directThreadModel.configureDirectThread.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }

    onEach(directThreadModel.commits.map { it.toUiCommits() }) {
      transitionTo { state, commits ->
        val commitsIds = commits.mapTo(mutableSetOf()) { it.source.id }
        val selectedCommitIds = state.selectedCommitIds.filter { it in commitsIds }
        val menuCommit = state.focusedMessage?.takeIf { it.source.id in commitsIds }
        val editingMessage = state.editingMessage?.takeIf { it.source.id in commitsIds }
        state.copy(
          focusedMessage = menuCommit,
          editingMessage = editingMessage
        ).updateSelection(
          commits = commits,
          selectedCommitIds = selectedCommitIds
        )
      }
      action { prevState, newState, _ ->
        // Цель редактирования исчезла из ленты (удалили здесь или на другом устройстве) —
        // режим уже сброшен транзишеном выше, осталось объяснить это пользователю.
        if (prevState.editingMessage != null && newState.editingMessage == null) {
          sendViewEvent(Snackbar(message = resRef(R.string.thread_edit_target_deleted)))
        }
      }
    }

    onEach(intent(ViewIntents::markReadUpTo)) {
      action { _, _, lastReadAt ->
        directThreadModel.markReadUpTo(lastReadAt)
      }
    }

    onEach(intent(ViewIntents::loadCommitsHistory)) {
      action { _, _, _ ->
        directThreadModel.fetchCommitHistory.start(queueingStrategy = QueueingStrategy.SkipNew)
      }
    }

    onEach(directThreadModel.hasCommitsHistory) {
      transitionTo { state, hasCommitsHistory ->
        state.copy(hasCommitsHistory = hasCommitsHistory)
      }
    }

    onEach(directThreadModel.fetchCommitHistory.jobFlow.state.map { it == JobState.Running }) {
      transitionTo { state, loading ->
        state.copy(loadingCommitsHistory = loading)
      }
    }

    onEach(directThreadModel.fetchCommitHistory.jobFlow.errors()) {
      action { _, _, _ ->
        sendViewEvent(
          Snackbar(
            isError = true,
            message = resRef(R.string.thread_load_history_failed)
          )
        )
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
    configureCommitEditTransitions()
    configureCommitDeletionTransitions()
  }

  private fun MachineDsl<ViewState>.configureCommitEditTransitions() {
    onEach(intent(ViewIntents::startEditMessage)) {
      transitionTo { state, commit ->
        state.copy(
          editingMessage = commit.takeIf { it.source.isSelf },
          focusedMessage = null
        )
      }
    }

    onEach(intent(ViewIntents::cancelEditMessage)) {
      transitionTo { state, _ ->
        state.copy(editingMessage = null)
      }
    }

    onEach(intent(ViewIntents::submitEditMessage)) {
      action { state, _, text ->
        val editingMessage = state.editingMessage ?: return@action
        val trimmed = text.trim()
        // UI гасит кнопку для пустого и неизменённого текста; здесь тот же гейт на случай гонки.
        if (trimmed.isEmpty() || trimmed == editingMessage.source.text) {
          return@action
        }
        directThreadModel.editCommit.start(
          argument1 = editingMessage.source.id,
          argument2 = trimmed
        )
      }
    }

    onEach(directThreadModel.editCommit.jobFlow.successResults()) {
      transitionTo { state, _ ->
        state.copy(editingMessage = null)
      }
    }

    onEach(directThreadModel.editCommit.jobFlow.errors()) {
      action { _, _, error ->
        if (error !is EditTargetNotFoundException) {
          sendViewEvent(
            Snackbar(
              isError = true,
              message = resRef(R.string.thread_edit_failed)
            )
          )
        }
      }
    }
  }

  private fun MachineDsl<ViewState>.configureCommitMenuTransitions() {
    onEach(intent(ViewIntents::showMessageMenu)) {
      transitionTo { state, commit ->
        state.copy(focusedMessage = commit)
      }
    }

    onEach(intent(ViewIntents::hideMessageMenu)) {
      transitionTo { state, _ ->
        state.copy(focusedMessage = null)
      }
    }

    onEach(intent(ViewIntents::copyMessage)) {
      action { _, _, _ ->
        sendViewEvent(
          Snackbar(message = resRef(R.string.thread_message_copied))
        )
      }
    }
  }

  private fun MachineDsl<ViewState>.configureSendMessageTransitions() {
    onEach(intent(ViewIntents::sendMessage)) {
      action { _, _, text ->
        directThreadModel.sendMessage(text.trim())
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

    onEach(intent(ViewIntents::showBranches)) {
      action { _, _, _ ->
        sendViewEvent(showBranchesModalSheet())
      }
    }

    onEach(intent(ViewIntents::openBranch)) {
      action { _, _, branchId ->
        eventSink.sendEvent(FlowEvent.BranchRequested(branchId))
      }
    }

    onEach(intent(ViewIntents::confirmCreateBranchParams)) {
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
      action { _, _, branchId ->
        eventSink.sendEvent(FlowEvent.BranchRequested(branchId))
      }
    }

    onEach(directThreadModel.createBranch.jobFlow.errors()) {
      action { _, _, _ ->
        val viewEvent = Snackbar(
          isError = true,
          message = resRef(R.string.thread_create_branch_creation_failed)
        )
        sendViewEvent(viewEvent)
      }
    }
  }

  private fun MachineDsl<ViewState>.configureSelectionTransitions() {
    onEach(intent(ViewIntents::disableEditMode)) {
      transitionTo { state, _ ->
        state.updateSelection(
          selectedCommitIds = emptyList()
        )
      }
    }

    onEach(intent(ViewIntents::toggleMessageSelection)) {
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

    onEach(intent(ViewIntents::confirmDeleteCommit)) {
      action { _, _, deleteCommits ->
        directThreadModel.deleteCommits.start(
          deleteCommits.ids,
          deleteCommits.forEveryone
        )
      }
    }

    onEach(directThreadModel.deleteCommits.jobFlow.successResults()) {
      transitionTo { state, _ ->
        state.updateSelection(
          selectedCommitIds = emptyList()
        )
      }
    }

    onEach(directThreadModel.deleteCommits.jobFlow.errors()) {
      action { _, _, _ ->
        val viewEvent = Snackbar(
          isError = true,
          message = resRef(R.string.thread_delete_failed)
        )
        sendViewEvent(viewEvent)
      }
    }
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
    commits = commits.map { commit ->
      Commit.message.bubble.isSelected.set(commit, commit.source.id in selectedIds)
    }
  )
}
