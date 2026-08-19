package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject
import ru.dimsuz.unicorn2.Machine
import ru.dimsuz.unicorn2.MachineDsl
import ru.dimsuz.unicorn2.machine
import ru.kode.remo.QueueingStrategy
import ru.kode.remo.errors
import ru.kode.remo.successResults
import ru.sla.clarify.core.domain.asLceState
import ru.sla.clarify.core.domain.entity.EditedMessage
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.startOnSubscribe
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.FlowEventSink
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.screen.ViewModel
import ru.sla.clarify.core.ui.toUiLceState
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.direct.thread.domain.DirectThreadModel
import ru.sla.clarify.feature.chat.direct.thread.domain.entity.EditTargetNotFoundException
import ru.sla.clarify.feature.chat.direct.thread.ui.routing.FlowEvent
import ru.sla.clarify.mapper.ui.toUiCommits
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.uikit.component.chat.Textual
import ru.sla.clarify.uikit.component.chat.message
import ru.sla.clarify.uikit.component.chat.selected
import ru.sla.clarify.uikit.event.Snackbar
import ru.sla.resourcerefs.resRef
import ru.sla.clarify.entity.chat.Commit as DomainCommit

class DirectThreadViewModel(
  dispatcher: CoroutineDispatcher,
  private val eventSink: FlowEventSink,
  private val directThreadModel: DirectThreadModel
) : ViewModel<ViewState, ViewIntents>(dispatcher) {

  @Inject
  constructor(
    eventSink: FlowEventSink,
    directThreadModel: DirectThreadModel
  ) : this(Dispatchers.Default, eventSink, directThreadModel)

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

    onEach(
      directThreadModel.configureDirectThread.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(contentLoadState = contentLoadState)
      }
    }

    onEach(directThreadModel.peer.filterNotNull()) {
      transitionTo { state, peer ->
        state.copy(peer = peer)
      }
    }

    onEach(
      combine(
        directThreadModel.commits,
        directThreadModel.peer
      ) { commits, peer ->
        commits.toUiCommits(memberNames = peer.toMemberNames())
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
        } else if (prevState.replyingCommit != null && newState.replyingCommit == null) {
          sendViewEvent(Snackbar(message = resRef(R.string.thread_reply_target_deleted)))
        }
      }
    }

    onEach(intent(ViewIntents::markMessageAsRead)) {
      action { _, _, lastReadAt ->
        directThreadModel.markReadUpTo(lastReadAt)
      }
    }

    onEach(directThreadModel.unreadCount) {
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
    configureBranchTransitions()
    configureSelectionTransitions()
  }

  private fun MachineDsl<ViewState>.configureCommitHistoryTransitions() {
    onEach(directThreadModel.hasCommitsHistory) {
      transitionTo { state, hasCommitsHistory ->
        state.copy(canLoadCommitsHistory = hasCommitsHistory)
      }
    }

    onEach(
      directThreadModel.fetchCommitHistory.jobFlow
        .asLceState()
        .map { it.toUiLceState() }
    ) {
      transitionTo { state, contentLoadState ->
        state.copy(loadingCommitsHistory = contentLoadState is ContentLoadState.Loading)
      }
    }

    onEach(directThreadModel.fetchCommitHistory.jobFlow.errors()) {
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
        directThreadModel.fetchCommitHistory.start(
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
      action { _, _, text ->
        directThreadModel.sendMessage(
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
        directThreadModel.sendMessage(
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
        // state здесь — прошлое состояние (ещё с editingMessage), transitionTo его уже обнулил.
        val editingMessage = state.editingCommit ?: return@action
        EditedMessage.validate(
          text = text,
          original = (editingMessage as? Textual)?.text
        ).onRight { edited ->
          directThreadModel.editCommit.start(
            argument1 = editingMessage.source.id,
            argument2 = edited.value
          )
        }
      }
    }

    onEach(directThreadModel.editCommit.jobFlow.errors()) {
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
        directThreadModel.deleteCommits.start(
          deleteCommits.ids,
          deleteCommits.forEveryone
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

    onEach(intent(ViewIntents::openChronology)) {
      action { state, _, _ ->
        val peerId = state.peer?.id ?: return@action
        eventSink.sendEvent(FlowEvent.ChronologyRequested(peerId))
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
}

private fun Peer?.toMemberNames(): Map<UserId, String?> {
  return this?.let { mapOf(UserId(it.id.value) to it.displayName) }.orEmpty()
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
