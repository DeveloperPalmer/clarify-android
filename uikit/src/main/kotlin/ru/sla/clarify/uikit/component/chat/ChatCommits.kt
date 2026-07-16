package ru.sla.clarify.uikit.component.chat

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.component.InviteMemberItem
import ru.sla.clarify.uikit.component.bubble.BubbleMessageItem
import ru.sla.clarify.uikit.component.menu.AnchoredCommitMenu
import ru.sla.clarify.uikit.component.menu.CommitMenuAction
import ru.sla.resourcerefs.compose.resolveTextRef
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * [commitMenu] is the message whose bubble currently shows the anchored context menu; menu
 * actions report the message back via [onCreateBranch] / [onCopyMessage] / [onSelectMessage]
 * and every action (or a dismiss) ends with [onCloseCommitMenu]. After [commitMenu] turns
 * null the menu stays composed until its exit animation finishes, only then it unmounts.
 */
@Composable
fun ChatCommits(
  commits: List<Commit>,
  listState: LazyListState,
  onCommitsRead: (LocalDateTime) -> Unit,
  modifier: Modifier = Modifier,
  commitMenu: Commit.Message? = null,
  onCommitClick: ((Commit.Message) -> Unit)? = null,
  onCommitLongClick: ((Commit.Message) -> Unit)? = null,
  onCloseCommitMenu: (() -> Unit)? = null,
  onCreateBranch: ((Commit.Message) -> Unit)? = null,
  onCopyMessage: ((Commit.Message) -> Unit)? = null,
  onSelectMessage: ((Commit.Message) -> Unit)? = null
) {
  // The menu bubble keeps its slot while the exit animation plays: the last menu commit is
  // latched here and released once the popup reports through onHidden that it has fully hidden.
  var displayedMenuCommit by remember { mutableStateOf<Commit.Message?>(null) }
  displayedMenuCommit = commitMenu ?: displayedMenuCommit

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
    verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Bottom)
  ) {
    itemsIndexed(
      items = commits,
      key = { _, item -> item.source.id.value }
    ) { _, commit ->
      when (commit) {
        is Commit.Message -> {
          val showsMenu = displayedMenuCommit?.source?.id == commit.source.id
          BubbleMessageItem(
            modifier = Modifier.animateItem(),
            bubble = commit.bubble,
            onClick = onCommitClick?.let { handler -> { handler(commit) } },
            onLongClick = onCommitLongClick?.let { handler -> { handler(commit) } },
            menu = if (showsMenu) {
              {
                CommitMenuPopup(
                  commit = commit,
                  visible = commitMenu != null,
                  onHidden = { displayedMenuCommit = null },
                  onCreateBranch = onCreateBranch,
                  onCopyMessage = onCopyMessage,
                  onSelectMessage = onSelectMessage,
                  onClose = onCloseCommitMenu
                )
              }
            } else {
              null
            }
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

@Composable
private fun CommitMenuPopup(
  commit: Commit.Message,
  visible: Boolean,
  onHidden: () -> Unit,
  onCreateBranch: ((Commit.Message) -> Unit)?,
  onCopyMessage: ((Commit.Message) -> Unit)?,
  onSelectMessage: ((Commit.Message) -> Unit)?,
  onClose: (() -> Unit)?
) {
  fun menuAction(action: ((Commit.Message) -> Unit)?): () -> Unit = {
    action?.invoke(commit)
    onClose?.invoke()
  }
  AnchoredCommitMenu(
    visible = visible,
    onHidden = onHidden,
    bottomSafePadding = MenuBottomSafePadding,
    onDismiss = { onClose?.invoke() },
    actions = listOf(
      CommitMenuAction(
        iconRes = R.drawable.ic_git_fork_24,
        label = stringResource(R.string.thread_menu_create_branch),
        onClick = menuAction(onCreateBranch)
      ),
      CommitMenuAction(
        iconRes = R.drawable.ic_copy_24,
        label = stringResource(R.string.thread_menu_copy),
        onClick = menuAction(onCopyMessage)
      ),
      CommitMenuAction(
        iconRes = R.drawable.ic_select_24,
        label = stringResource(R.string.thread_menu_select),
        onClick = menuAction(onSelectMessage)
      )
    )
  )
}

private val MenuBottomSafePadding = 96.dp
