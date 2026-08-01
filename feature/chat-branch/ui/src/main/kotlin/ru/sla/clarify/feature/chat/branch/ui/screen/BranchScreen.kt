package ru.sla.clarify.feature.chat.branch.ui.screen

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.entity.chat.Branch.MergeRequest.Status
import ru.sla.clarify.feature.chat.branch.ui.components.ChatEmptyState
import ru.sla.clarify.feature.chat.branch.ui.components.MergeRequestButton
import ru.sla.clarify.feature.chat.branch.ui.components.MergeRequestCard
import ru.sla.clarify.uikit.component.button.ChatScrollToBottomButton
import ru.sla.clarify.uikit.component.chat.ChatCommits
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.uikit.component.chat.Textual
import ru.sla.clarify.uikit.component.icon.IconAction
import ru.sla.clarify.uikit.component.popup.PopupScrim
import ru.sla.clarify.uikit.component.scrim.ScrimEffect
import ru.sla.clarify.uikit.component.textfield.ChatTextField
import ru.sla.clarify.uikit.component.textfield.ChatTextFieldDefaults
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.component.topappbar.rememberTopBarElevation
import ru.sla.clarify.uikit.modifier.bottomShadow
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.resRef

@Composable
fun BranchScreen(viewModel: BranchViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    val scaffoldState = rememberScreenScaffoldState()
    scaffoldState.contentLoadState = state.contentLoadState
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    BackHandler(
      enabled = state.selectionEnabled,
      onBack = intents.disableSelectionMode
    )
    BackHandler(
      enabled = state.focusedCommit != null,
      onBack = intents.hideMessageMenu
    )
    ScreenScaffold(scaffoldState) {
      BranchReadyContent(
        modifier = Modifier.fillMaxSize(),
        state = state,
        intents = intents,
        onSelectCommit = intents.toggleSelectionMode,
        onDeleteCommit = intents.deleteCommit,
        onClosePopup = intents.hideMessageMenu,
        onCopyCommit = { text ->
          scope.launch {
            val clipData = ClipData.newPlainText(null, text)
            clipboard.setClipEntry(clipData.toClipEntry())
            intents.copyMessage()
          }
        },
        onCommitLongClick = intents.toggleSelectionMode,
        onCommitClick = { commit ->
          if (state.selectionEnabled) {
            intents.toggleSelectionMode(commit)
          } else {
            intents.showMessageMenu(commit)
          }
        }
      )
      ScrimEffect(
        visible = state.cardShown,
        onFinish = intents.hideMergeRequest
      )
      MergeRequestCard(
        modifier = Modifier
          .systemBarsPadding()
          .padding(start = 12.dp, end = 12.dp),
        visible = state.cardShown,
        mergeRequest = state.mergeRequest,
        members = state.members,
        approvers = state.approvers,
        isCurrentUserApproved = state.isCurrentUserApproved,
        mergeRequestInProgress = state.mergeRequestInProgress,
        onApprove = intents.approveMergeRequest,
        onRevoke = intents.revokeApprovalMergeRequest,
        onCancel = intents.cancelMergeRequest,
        onFinalize = intents.finalizeMergeRequest
      )
    }
  }
}

@Composable
internal fun BranchReadyContent(
  state: ViewState,
  intents: ViewIntents,
  onCommitClick: (Commit) -> Unit,
  onCommitLongClick: (Commit) -> Unit,
  onClosePopup: () -> Unit,
  onCopyCommit: (String) -> Unit,
  onSelectCommit: (Commit) -> Unit,
  onDeleteCommit: (Commit) -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(enabled = state.cardShown) {
    intents.hideMergeRequest()
  }
  Box(modifier) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .imePadding()
    ) {
      val listState = rememberLazyListState()
      val topBarElevation = rememberTopBarElevation(listState)
      if (state.selectionEnabled) {
        SelectionTopAppBar(
          modifier = Modifier.bottomShadow { topBarElevation.value },
          selectedCount = state.selectedCommitIds.size,
          onClose = intents.disableSelectionMode,
          onDelete = intents.deleteCommits
        )
      } else {
        TopAppBar(
          modifier = Modifier.bottomShadow { topBarElevation.value },
          navigationIcon = { TopAppBarDefaults.NavigationIcon(intents.navigateBack) },
          title = { state.branchName?.let { TopAppBarCenterContent(branchName = it) } },
          actions = {
            MergeRequestButton(
              visible = !state.cardShown,
              inProgress = state.mergeRequestInProgress,
              status = state.mergeRequest?.status,
              onOpenMergeRequest = intents.openMergeRequest
            )
          }
        )
      }
      Box(modifier = Modifier.weight(1f)) {
        ChatCommits(
          modifier = Modifier.fillMaxSize(),
          listState = listState,
          items = state.commits,
          selectionEnabled = state.selectionEnabled,
          focused = state.focusedCommit,
          hasHistory = state.canLoadCommitsHistory,
          loadingHistory = state.loadingCommitsHistory,
          onClick = onCommitClick,
          onLongClick = onCommitLongClick,
          onLoad = intents.loadCommitsHistory,
          onRead = intents.markMessageAsRead,
          onEdit = intents.showEditMessage,
          onCopy = { commit -> (commit as? Textual)?.text?.let(onCopyCommit) },
          onSelect = onSelectCommit,
          onDelete = onDeleteCommit,
          onClosePopup = onClosePopup
        )
        ChatScrollToBottomButton(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
          listState = listState,
          unreadCount = state.unreadCount
        )
        ChatEmptyState(
          modifier = Modifier.fillMaxSize(),
          visible = state.commits.isEmpty(),
          text = stringResource(R.string.branch_empty_state)
        )
      }
      BottomArea(
        mergeRequestStatus = state.mergeRequest?.status,
        editingMessage = state.editingCommit,
        onSend = intents.sendMessage,
        onSubmitEdit = intents.confirmEditMessage,
        onCancelEdit = intents.hideEditMessage
      )
    }
    PopupScrim(
      visible = state.focusedCommit != null,
      onDismiss = onClosePopup
    )
  }
}

@Composable
private fun SelectionTopAppBar(
  selectedCount: Int,
  onClose: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    modifier = modifier,
    navigationIcon = {
      IconAction(
        iconResId = R.drawable.ic_close_24,
        iconTint = AppTheme.colors.contentPrimary,
        onClick = onClose
      )
    },
    title = {
      Text(
        text = stringResource(R.string.thread_selection_count, selectedCount),
        style = AppTheme.typography.title2Bold,
        color = AppTheme.colors.contentPrimary
      )
    },
    actions = {
      IconAction(
        iconResId = R.drawable.ic_trash_24,
        iconTint = AppTheme.colors.errorPrimary,
        onClick = onDelete
      )
    }
  )
}

@Composable
private fun TopAppBarCenterContent(
  branchName: String,
  modifier: Modifier = Modifier
) {
  Text(
    modifier = modifier,
    text = branchName.ifEmpty { stringResource(R.string.branch_default_name) },
    style = AppTheme.typography.title2Bold
  )
}

@Composable
private fun BottomArea(
  mergeRequestStatus: Status?,
  editingMessage: Commit?,
  onSend: (String) -> Unit,
  onSubmitEdit: (String) -> Unit,
  onCancelEdit: () -> Unit
) {
  when (mergeRequestStatus) {
    null -> {
      ChatComposer(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        editingMessage = editingMessage,
        onSend = onSend,
        onSubmitEdit = onSubmitEdit,
        onCancelEdit = onCancelEdit
      )
    }
    Status.Open,
    Status.ReadyToMerge -> {
      LockedMessage(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.branch_locked_merge_in_progress)
      )
    }
    Status.Merged -> {
      LockedMessage(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.branch_merged_read_only)
      )
    }
  }
}

@Composable
private fun ChatComposer(
  editingMessage: Commit?,
  onSend: (String) -> Unit,
  onSubmitEdit: (String) -> Unit,
  onCancelEdit: () -> Unit,
  modifier: Modifier = Modifier
) {
  var inputValue by rememberSaveable { mutableStateOf("") }
  val editingSource = editingMessage?.source
  val editingText = (editingMessage as? Textual)?.text
  // Вход в режим и пере-выбор цели перезаписывают поле текстом оригинала, выход очищает —
  // черновики осознанно не сохраняются. Сравнение с прошлым id (а не LaunchedEffect от null)
  // не даёт затереть восстановленный rememberSaveable-текст при пересоздании экрана.
  var lastEditingId by rememberSaveable { mutableStateOf<String?>(null) }
  if (editingSource?.id?.value != lastEditingId) {
    lastEditingId = editingSource?.id?.value
    inputValue = editingText.orEmpty()
  }
  ChatTextField(
    modifier = modifier,
    value = inputValue,
    onValueChange = { inputValue = it },
    sendEnabled = editingSource == null || inputValue.trim() != editingText,
    sendIconRes = if (editingSource != null) R.drawable.ic_check_24 else R.drawable.ic_send_24,
    // Ключ меняется при входе/выходе/пере-выборе цели — программно заменённый текст
    // проявляется мягко, обычная печать не фейдится.
    contentFadeKey = editingSource?.id?.value,
    // Вход в режим редактирования ставит фокус в поле, поднимает клавиатуру и уводит каретку
    // в конец текста. null вне режима — фокус не запрашивается при обычной отправке.
    focusRequestKey = editingSource?.id?.value,
    placeholder = if (editingSource != null) {
      resRef(R.string.thread_edit_empty_placeholder)
    } else {
      resRef(R.string.chat_input_placeholder)
    },
    header = editingText?.let { text ->
      {
        ChatTextFieldDefaults.EditHeader(
          text = text,
          onClose = onCancelEdit
        )
      }
    },
    onSend = {
      if (editingSource != null) {
        onSubmitEdit(inputValue)
      } else {
        onSend(inputValue)
      }
    },
    // При редактировании поле не чистим: ошибка отправки не должна терять правку;
    // успех очистит его сам через сброс режима.
    onClear = {
      if (editingSource == null) {
        inputValue = ""
      }
    }
  )
}

@Composable
private fun LockedMessage(
  text: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      style = AppTheme.typography.body2,
      color = AppTheme.colors.contentPrimary
    )
  }
}
