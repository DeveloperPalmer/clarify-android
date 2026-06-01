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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chat.thread.ui.components.ChatEmptyState
import ru.sla.clarify.feature.chat.thread.ui.components.ChatInput
import ru.sla.clarify.feature.chat.thread.ui.components.Commits
import ru.sla.clarify.feature.chat.thread.ui.components.ScrollToBottomFab
import ru.sla.clarify.feature.chat.thread.ui.components.rememberTopBarElevation
import ru.sla.clarify.feature.chat.thread.ui.entity.Commit
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.uikit.component.Avatar
import ru.sla.clarify.uikit.component.button.TertiaryIconButtonSmall
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
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
    ScreenScaffold(state = scaffoldState) {
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
        onCommitLongClick = intents.showBranches,
        onCommitsRead = intents.markReadUpTo,
        onSend = intents.sendMessage
      )
    }
  }
}

@Composable
internal fun ThreadReadyContent(
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
  val scope = rememberCoroutineScope()
  val keyboardController = rememberKeyboardController()
  val listState = rememberLazyListState()
  val topBarElevation = rememberTopBarElevation(listState)
  val topBarModifier = remember(topBarElevation) {
    Modifier.bottomShadow { topBarElevation.value }
  }
  Column(modifier) {
    TopAppBar(
      modifier = topBarModifier,
      navigationIcon = { TopAppBarDefaults.NavigationIcon(onBack) },
      title = { peer?.let { TopAppBarContent(peer = peer) } },
      actions = {
        TertiaryIconButtonSmall(
          modifier = Modifier.padding(end = 4.dp),
          iconRes = R.drawable.ic_branch_24,
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
    if (commits.isEmpty()) {
      ChatEmptyState(
        text = stringResource(R.string.thread_empty_state),
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
    } else {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        Commits(
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
        ScrollToBottomFab(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
          listState = listState,
          unreadCount = unreadCount
        )
      }
    }
    ChatInput(
      onSend = onSend,
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
private fun TopAppBarContent(
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
