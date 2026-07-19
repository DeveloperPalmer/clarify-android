package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.direct.thread.ui.components.ChatEmptyState
import ru.sla.clarify.uikit.component.avatar.Avatar
import ru.sla.clarify.uikit.component.button.ChatScrollToBottomButton
import ru.sla.clarify.uikit.component.button.TertiaryIconButtonSmall
import ru.sla.clarify.uikit.component.chat.ChatCommits
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.uikit.component.icon.IconAction
import ru.sla.clarify.uikit.component.popup.PopupScrim
import ru.sla.clarify.uikit.component.textfield.ChatTextField
import ru.sla.clarify.uikit.component.textfield.ChatTextFieldDefaults
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.component.topappbar.rememberTopBarElevation
import ru.sla.clarify.uikit.keyboard.rememberKeyboardController
import ru.sla.clarify.uikit.modifier.bottomShadow
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.resRef
import java.time.LocalDateTime

@Composable
fun DirectThreadScreen(viewModel: DirectThreadViewModel) {
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
      onBack = intents.disableEditMode
    )
    BackHandler(
      enabled = state.focusedMessage != null,
      onBack = intents.hideMessageMenu
    )
    ScreenScaffold(scaffoldState) {
      DirectThreadReadyContent(
        modifier = Modifier.fillMaxSize(),
        state = state,
        onBack = intents.navigateBack,
        onClose = intents.disableEditMode,
        onShowBranches = intents.showBranches,
        onLoadMore = intents.loadCommitsHistory,
        onDeleteCommit = intents.deleteCommit,
        onDeleteCommits = intents.deleteCommits,
        onCloseMessageMenu = intents.hideMessageMenu,
        onCreateBranch = intents.createBranch,
        onEditMessage = intents.startEditMessage,
        onSubmitEdit = intents.submitEditMessage,
        onCancelEdit = intents.cancelEditMessage,
        onSelectMessage = intents.toggleMessageSelection,
        onCopyMessage = { commit ->
          scope.launch {
            val clipData = ClipData.newPlainText(null, commit.bubble.text)
            clipboard.setClipEntry(clipData.toClipEntry())
            intents.copyMessage()
          }
        },
        onCommitsRead = intents.markReadUpTo,
        onSend = intents.sendMessage,
        onCommitLongClick = intents.toggleMessageSelection,
        onCommitClick = { commit ->
          if (state.selectionEnabled) {
            intents.toggleMessageSelection(commit)
          } else {
            intents.showMessageMenu(commit)
          }
        }
      )
    }
  }
}

@Composable
private fun DirectThreadReadyContent(
  state: ViewState,
  onBack: () -> Unit,
  onClose: () -> Unit,
  onSend: (String) -> Unit,
  onShowBranches: () -> Unit,
  onLoadMore: () -> Unit,
  onCommitClick: (Commit.Message) -> Unit,
  onCommitLongClick: (Commit) -> Unit,
  onCommitsRead: (LocalDateTime) -> Unit,
  onCloseMessageMenu: () -> Unit,
  onDeleteCommits: () -> Unit,
  onDeleteCommit: (Commit) -> Unit,
  onCreateBranch: (Commit.Message) -> Unit,
  onEditMessage: (Commit.Message) -> Unit,
  onSubmitEdit: (String) -> Unit,
  onCancelEdit: () -> Unit,
  onCopyMessage: (Commit.Message) -> Unit,
  onSelectMessage: (Commit) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .imePadding()
    ) {
      val scope = rememberCoroutineScope()
      val listState = rememberLazyListState()
      val topBarElevation = rememberTopBarElevation(listState)
      val keyboardController = rememberKeyboardController()
      if (state.selectionEnabled) {
        SelectionTopAppBar(
          modifier = Modifier.bottomShadow { topBarElevation.value },
          selectedCount = state.selectedCommitIds.size,
          onClose = onClose,
          onDelete = onDeleteCommits
        )
      } else {
        TopAppBar(
          modifier = Modifier.bottomShadow { topBarElevation.value },
          navigationIcon = { TopAppBarDefaults.NavigationIcon(onBack) },
          title = { state.peer?.let { TopAppBarCenterContent(it) } },
          actions = {
            TertiaryIconButtonSmall(
              modifier = Modifier.padding(end = 4.dp),
              iconRes = R.drawable.ic_git_branch_24,
              text = stringResource(R.string.thread_branches_count, state.branches.size),
              onClick = {
                scope.launch {
                  keyboardController.awaitHide()
                  onShowBranches()
                }
              }
            )
          }
        )
      }
      Box(modifier = Modifier.weight(1f)) {
        ChatCommits(
          modifier = Modifier.fillMaxSize(),
          listState = listState,
          commits = state.commits,
          onLoadMore = onLoadMore,
          hasCommitsHistory = state.hasCommitsHistory,
          loadingCommitsHistory = state.loadingCommitsHistory,
          selectionEnabled = state.selectionEnabled,
          focusedMessage = state.focusedMessage,
          onCommitsRead = onCommitsRead,
          onMessageClick = onCommitClick,
          onMessageLongClick = onCommitLongClick,
          onCloseMessagePopup = onCloseMessageMenu,
          onCreateBranch = onCreateBranch,
          onEditMessage = onEditMessage,
          onCopyMessage = onCopyMessage,
          onSelectMessage = onSelectMessage,
          onDeleteCommit = onDeleteCommit
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
          text = stringResource(R.string.thread_empty_state)
        )
      }
      BottomArea(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        editingMessage = state.editingMessage,
        onSend = onSend,
        onSubmitEdit = onSubmitEdit,
        onCancelEdit = onCancelEdit
      )
    }
    PopupScrim(
      visible = state.focusedMessage != null,
      onDismiss = onCloseMessageMenu
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
  peer: Peer,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Avatar(
      size = 40.dp,
      photoUrl = peer.photoUrl,
      fallback = peer.displayName
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = peer.displayName,
        style = AppTheme.typography.title2Bold
      )
    }
  }
}

@Composable
private fun BottomArea(
  editingMessage: Commit.Message?,
  onSend: (String) -> Unit,
  onSubmitEdit: (String) -> Unit,
  onCancelEdit: () -> Unit,
  modifier: Modifier = Modifier
) {
  var inputValue by rememberSaveable { mutableStateOf("") }
  val editingSource = editingMessage?.source
  // Вход в режим и пере-выбор цели перезаписывают поле текстом оригинала, выход очищает —
  // черновики осознанно не сохраняются. Сравнение с прошлым id (а не LaunchedEffect от null)
  // не даёт затереть восстановленный rememberSaveable-текст при пересоздании экрана.
  var lastEditingId by rememberSaveable { mutableStateOf<String?>(null) }
  if (editingSource?.id?.value != lastEditingId) {
    lastEditingId = editingSource?.id?.value
    inputValue = editingSource?.text.orEmpty()
  }
  ChatTextField(
    modifier = modifier,
    value = inputValue,
    onValueChange = { inputValue = it },
    sendEnabled = editingSource == null || inputValue.trim() != editingSource.text,
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
    header = editingMessage?.let { editing ->
      {
        ChatTextFieldDefaults.EditHeader(
          message = editing.source,
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
