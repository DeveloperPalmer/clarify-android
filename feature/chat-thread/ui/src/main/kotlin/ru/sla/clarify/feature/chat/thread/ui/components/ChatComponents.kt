package ru.sla.clarify.feature.chat.thread.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chat.thread.ui.entity.Commit
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.clarify.uikit.component.textfield.ChatTextField
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import java.time.LocalDateTime

@Composable
internal fun rememberTopBarElevation(listState: LazyListState): State<Dp> {
  val isScrolledUnderTopBar by remember { derivedStateOf { listState.canScrollForward } }
  val elevation = animateDpAsState(
    label = "topBarElevation",
    targetValue = if (isScrolledUnderTopBar) 4.dp else 0.dp
  )
  return elevation
}

@Composable
internal fun Commits(
  commits: List<Commit>,
  listState: LazyListState,
  onCommitsRead: (LocalDateTime) -> Unit,
  modifier: Modifier = Modifier,
  onCommitLongClick: ((Commit.Message) -> Unit)? = null
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
    if (newestCommit == null || newestCommitId == null) {
      return@LaunchedEffect
    }
    val isFirstLoad = previousNewestCommitId == null
    val isNewCommit = newestCommitId != previousNewestCommitId
    previousNewestCommitId = newestCommitId
    if (!isNewCommit) {
      return@LaunchedEffect
    }
    val newestIsSelf = newestCommit.source.isSelf
    val wasAtBottom = listState.firstVisibleItemIndex <= 1
    if (isFirstLoad || newestIsSelf || wasAtBottom) {
      listState.animateScrollToItem(0)
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
          BubbleMessage(
            bubble = commit.bubble,
            onLongClick = { onCommitLongClick?.invoke(commit) }
          )
        }
      }
    }
  }
}

@Composable
internal fun ScrollToBottomFab(
  listState: LazyListState,
  unreadCount: Int,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val isVisible by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
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
            onClick = {
              scope.launch {
                if (listState.firstVisibleItemIndex > SCROLL_TO_BOTTOM_ANIMATE_THRESHOLD) {
                  listState.scrollToItem(SCROLL_TO_BOTTOM_ANIMATE_THRESHOLD)
                }
                listState.animateScrollToItem(0)
              }
            }
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
internal fun ChatEmptyState(
  text: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      style = AppTheme.typography.body1
    )
  }
}

@Composable
internal fun ChatInput(
  onSend: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var inputValue by rememberSaveable { mutableStateOf("") }
  ChatTextField(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 8.dp),
    value = inputValue,
    onValueChange = { inputValue = it },
    onSend = { onSend(inputValue) },
    onClear = { inputValue = "" }
  )
}

private const val SCROLL_TO_BOTTOM_ANIMATE_THRESHOLD = 10
