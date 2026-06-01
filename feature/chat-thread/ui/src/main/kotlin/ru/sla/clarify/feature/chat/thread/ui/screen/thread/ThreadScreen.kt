package ru.sla.clarify.feature.chat.thread.ui.screen.thread

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chat.thread.ui.entity.Commit
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.uikit.component.Avatar
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.clarify.uikit.component.button.TertiaryIconButtonSmall
import ru.sla.clarify.uikit.component.textfield.OutlinedTextField
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.modifier.bottomShadow
import ru.sla.clarify.uikit.modifier.surface
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
        isSending = state.isSending,
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
  isSending: Boolean,
  unreadCount: Int,
  onBack: () -> Unit,
  onSend: (String) -> Unit,
  onShowBranches: () -> Unit,
  onCommitLongClick: (Commit.Message) -> Unit,
  onCommitsRead: (LocalDateTime) -> Unit,
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()
  val isScrolledUnderTopBar by remember { derivedStateOf { listState.canScrollForward } }
  val topBarElevation by animateDpAsState(
    label = "topBarElevation",
    targetValue = if (isScrolledUnderTopBar) 4.dp else 0.dp
  )
  Column(modifier) {
    TopAppBar(
      modifier = Modifier.bottomShadow(topBarElevation),
      navigationIcon = { TopAppBarDefaults.NavigationIcon(onBack) },
      title = { peer?.let { TopAppBarContent(peer = peer) } },
      actions = {
        TertiaryIconButtonSmall(
          modifier = Modifier.padding(end = 4.dp),
          iconRes = R.drawable.ic_branch_24,
          onClick = onShowBranches,
          text = stringResource(R.string.thread_branches_count, branchesCount)
        )
      }
    )
    if (commits.isEmpty()) {
      TreadEmptyState(
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
          onLongClick = onCommitLongClick,
          onCommitsRead = onCommitsRead
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
    InputRow(
      modifier = Modifier.fillMaxWidth(),
      isSending = isSending,
      onSend = onSend
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

@Composable
private fun Commits(
  commits: List<Commit>,
  listState: LazyListState,
  onLongClick: (Commit.Message) -> Unit,
  onCommitsRead: (LocalDateTime) -> Unit,
  modifier: Modifier = Modifier
) {
  LaunchedEffect(listState, commits) {
    snapshotFlow {
      listState.layoutInfo.visibleItemsInfo
        .mapNotNull { commits.getOrNull(it.index)?.source?.timestamp }
        .maxOrNull()
    }
      .filterNotNull()
      .distinctUntilChanged()
      .collect(onCommitsRead)
  }
  val newestCommit = commits.firstOrNull()
  val newestCommitId = newestCommit?.source?.id?.value
  var previousNewestCommitId by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(newestCommitId) {
    if (newestCommit == null || newestCommitId == null) return@LaunchedEffect
    val isFirstLoad = previousNewestCommitId == null
    val isNewCommit = newestCommitId != previousNewestCommitId
    previousNewestCommitId = newestCommitId
    if (!isNewCommit) return@LaunchedEffect

    val newestIsSelf = newestCommit.source.isSelf
    val wasAtBottom = listState.firstVisibleItemIndex <= 1
    if (isFirstLoad || newestIsSelf || wasAtBottom) {
      listState.scrollToItem(0)
    }
  }
  LazyColumn(
    modifier = modifier,
    state = listState,
    reverseLayout = true,
    verticalArrangement = Arrangement.spacedBy(
      space = 4.dp,
      alignment = Alignment.Bottom
    ),
    contentPadding = PaddingValues(8.dp)
  ) {
    itemsIndexed(
      items = commits,
      key = { _, item -> item.source.id.value }
    ) { _, commit ->
      when (commit) {
        is Commit.Message -> {
          CommitMessageBubble(
            commit = commit,
            onLongClick = { onLongClick(commit) }
          )
        }
      }
    }
  }
}

@Composable
private fun ScrollToBottomFab(
  listState: LazyListState,
  unreadCount: Int,
  modifier: Modifier = Modifier
) {
  val isVisible by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
  }
  val scope = rememberCoroutineScope()
  AnimatedVisibility(
    modifier = modifier,
    visible = isVisible,
    enter = fadeIn() + scaleIn(),
    exit = fadeOut() + scaleOut()
  ) {
    Box(contentAlignment = Alignment.TopCenter) {
      Box(
        modifier = Modifier
          .padding(top = 8.dp)
          .size(44.dp)
          .surface(
            shape = CircleShape,
            backgroundColor = AppTheme.colors.cardSecondary,
            elevation = 4.dp,
            onClick = { scope.launch { listState.animateScrollToItem(0) } }
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painter = painterResource(R.drawable.ic_chevron_down_24),
          tint = AppTheme.colors.contentSecondary,
          contentDescription = stringResource(R.string.thread_scroll_to_bottom)
        )
      }
      if (unreadCount > 0) {
        UnreadBadge(
          modifier = Modifier.align(Alignment.TopCenter),
          count = unreadCount
        )
      }
    }
  }
}

@Composable
private fun UnreadBadge(
  count: Int,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
      .surface(
        shape = CircleShape,
        backgroundColor = AppTheme.colors.contentAccentPrimary
      )
      .padding(horizontal = 5.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = count.toString(),
      style = AppTheme.typography.caption,
      color = AppTheme.colors.contentAccentSecondary
    )
  }
}

@Composable
private fun TreadEmptyState(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = stringResource(R.string.thread_empty_state),
      style = AppTheme.typography.body1
    )
  }
  return
}

@Composable
private fun CommitMessageBubble(
  commit: Commit.Message,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = if (commit.bubble.side is BubbleMessage.Side.Right) {
      Alignment.CenterEnd
    } else {
      Alignment.CenterStart
    }
  ) {
    BubbleMessage(
      bubble = commit.bubble,
      modifier = Modifier
        .widthIn(max = 280.dp),
      onLongClick = onLongClick
    )
  }
}

@Composable
private fun InputRow(
  isSending: Boolean,
  onSend: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    var inputValue by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
      modifier = Modifier.weight(1f),
      value = inputValue,
      onValueChange = { inputValue = it },
      placeholder = { Text(stringResource(R.string.chat_input_placeholder)) }
    )
    Button(
      onClick = {
        val text = inputValue.trim()
        if (text.isNotEmpty()) {
          onSend(text)
          inputValue = ""
        }
      },
      enabled = inputValue.isNotBlank() && !isSending
    ) {
      Text(stringResource(R.string.chat_input_send_button))
    }
  }
}
