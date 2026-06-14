package ru.sla.clarify.feature.chat.group.thread.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import ru.sla.clarify.feature.chat.group.thread.ui.entity.Commit
import ru.sla.clarify.uikit.component.InviteParticipantItem
import ru.sla.clarify.uikit.component.bubble.BubbleMessageItem
import ru.sla.resourcerefs.compose.resolveTextRef
import java.time.LocalDateTime

@Composable
internal fun ChatCommits(
  commits: List<Commit>,
  listState: LazyListState,
  onCommitsRead: (LocalDateTime) -> Unit,
  modifier: Modifier = Modifier,
  onCommitLongClick: ((Commit.Message) -> Unit)? = null
) {
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

  val shownCommitIds = remember {
    commits.mapTo(mutableSetOf()) { it.source.id.value }
  }

  LazyColumn(
    modifier = modifier,
    state = listState,
    reverseLayout = true,
    contentPadding = PaddingValues(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)
  ) {
    itemsIndexed(
      items = commits,
      key = { _, item -> item.source.id.value }
    ) { _, commit ->
      when (commit) {
        is Commit.Message -> {
          AnimatedBubbleMessage(
            commit = commit,
            shownCommitIds = shownCommitIds,
            onLongClick = onCommitLongClick
          )
        }
        is Commit.InviteParticipant -> {
          InviteParticipantItem(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp),
            text = resolveTextRef(commit.text)
          )
        }
      }
    }
  }
}

@Composable
private fun LazyItemScope.AnimatedBubbleMessage(
  commit: Commit.Message,
  shownCommitIds: MutableSet<String>,
  onLongClick: ((Commit.Message) -> Unit)?
) {
  val commitId = commit.source.id.value
  val animateEntry = commitId !in shownCommitIds
  LaunchedEffect(commitId) { shownCommitIds += commitId }
  val viewState = remember {
    MutableTransitionState(initialState = !animateEntry).apply {
      targetState = true
    }
  }
  AnimatedVisibility(
    modifier = Modifier.animateItem(fadeInSpec = null),
    visibleState = viewState,
    enter = slideInVertically(initialOffsetY = { it }) + fadeIn()
  ) {
    BubbleMessageItem(
      bubble = commit.bubble,
      onLongClick = onLongClick?.let { handler -> { handler(commit) } }
    )
  }
}
