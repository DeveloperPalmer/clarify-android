package ru.sla.clarify.feature.chat.thread.ui.screen.thread

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chat.thread.ui.components.ChatCommits
import ru.sla.clarify.feature.chat.thread.ui.components.ChatEmptyState
import ru.sla.clarify.feature.chat.thread.ui.entity.Commit
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.uikit.component.Avatar
import ru.sla.clarify.uikit.component.button.ChatScrollToBottomButton
import ru.sla.clarify.uikit.component.button.TertiaryIconButtonSmall
import ru.sla.clarify.uikit.component.textfield.ChatTextField
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.component.topappbar.rememberTopBarElevation
import ru.sla.clarify.uikit.keyboard.rememberKeyboardController
import ru.sla.clarify.uikit.modifier.bottomShadow
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme
import java.time.LocalDateTime

@Composable
fun ThreadScreen(viewModel: ThreadViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    val scaffoldState = rememberScreenScaffoldState()
    scaffoldState.contentLoadState = state.contentLoadState
    BackHandler(
      onBack = intents.navigateBack
    )
    ScreenScaffold(scaffoldState) {
      ThreadReadyContent(
        modifier = Modifier
          .fillMaxSize()
          .systemBarsPadding()
          .imePadding(),
        peer = state.peer,
        commits = state.commits,
        branchesCount = state.branches.size,
        unreadCount = state.unreadCount,
        onBack = intents.navigateBack,
        onShowBranches = intents.showBranchesList,
        onCommitLongClick = intents.createBranch,
        onCommitsRead = intents.markReadUpTo,
        onSend = intents.sendMessage
      )
    }
  }
}

@Composable
private fun ThreadReadyContent(
  peer: Peer?,
  commits: List<Commit>,
  branchesCount: Int,
  unreadCount: Int,
  onBack: () -> Unit,
  onSend: (String) -> Unit,
  onShowBranches: () -> Unit,
  onCommitLongClick: (Commit.Message) -> Unit,
  onCommitsRead: (LocalDateTime) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val topBarElevation = rememberTopBarElevation(listState)
    val keyboardController = rememberKeyboardController()
    TopAppBar(
      modifier = Modifier.bottomShadow { topBarElevation.value },
      navigationIcon = { TopAppBarDefaults.NavigationIcon(onBack) },
      title = { peer?.let { TopAppBarCenterContent(peer = peer) } },
      actions = {
        TertiaryIconButtonSmall(
          modifier = Modifier.padding(end = 4.dp),
          iconRes = R.drawable.ic_git_branch_24,
          text = stringResource(R.string.thread_branches_count, branchesCount),
          onClick = {
            scope.launch {
              keyboardController.awaitHide()
              onShowBranches()
            }
          }
        )
      }
    )
    Box(modifier = Modifier.weight(1f)) {
      ChatCommits(
        modifier = Modifier.fillMaxSize(),
        listState = listState,
        commits = commits,
        onCommitsRead = onCommitsRead,
        onCommitLongClick = { commit ->
          scope.launch {
            keyboardController.awaitHide()
            onCommitLongClick(commit)
          }
        }
      )
      ChatScrollToBottomButton(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 16.dp, bottom = 16.dp),
        listState = listState,
        unreadCount = unreadCount
      )
      ChatEmptyState(
        modifier = Modifier.fillMaxSize(),
        visible = commits.isEmpty(),
        text = stringResource(R.string.thread_empty_state)
      )
    }
    BottomArea(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 8.dp),
      onSend = onSend
    )
  }
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
      fallbackInitial = peer.displayName
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
  modifier: Modifier = Modifier,
  onSend: (String) -> Unit
) {
  var inputValue by rememberSaveable { mutableStateOf("") }
  ChatTextField(
    modifier = modifier,
    value = inputValue,
    onValueChange = { inputValue = it },
    onSend = { onSend(inputValue) },
    onClear = { inputValue = "" }
  )
}
