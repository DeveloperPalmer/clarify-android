package ru.sla.clarify.uikit.component.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import ru.sla.clarify.uikit.component.InviteMemberItem
import ru.sla.clarify.uikit.component.bubble.BubbleMessageItem
import ru.sla.resourcerefs.compose.resolveTextRef
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ChatCommits(
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
        .asSequence()
        .filter { it.index <= commits.lastIndex }
        .maxOfOrNull { commits[it.index].source.timestamp }
    }
      .filterNotNull()
      .distinctUntilChanged()
      .debounce(300.milliseconds)
      .collect(onCommitsRead)
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
          BubbleMessageItem(
            modifier = Modifier.animateItem(),
            bubble = commit.bubble,
            onLongClick = onCommitLongClick?.let { handler -> { handler(commit) } }
          )
        }

        is Commit.InviteMember -> {
          InviteMemberItem(
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
