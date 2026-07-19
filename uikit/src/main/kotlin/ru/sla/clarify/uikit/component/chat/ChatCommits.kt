package ru.sla.clarify.uikit.component.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.component.InviteMemberItem
import ru.sla.clarify.uikit.component.bubble.BubbleMessageItem
import ru.sla.clarify.uikit.component.popup.Action
import ru.sla.clarify.uikit.component.popup.Popup
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.compose.resolveTextRef
import ru.sla.resourcerefs.resRef
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

@Composable
@Suppress("CyclomaticComplexMethod")
fun ChatCommits(
  listState: LazyListState,
  commits: List<Commit>,
  onCommitsRead: (LocalDateTime) -> Unit,
  modifier: Modifier = Modifier,
  hasCommitsHistory: Boolean = false,
  loadingCommitsHistory: Boolean = false,
  selectionEnabled: Boolean = false,
  focusedMessage: Commit.Message? = null,
  onLoadMore: () -> Unit = {},
  onMessageClick: ((Commit.Message) -> Unit)? = null,
  onMessageLongClick: ((Commit.Message) -> Unit)? = null,
  onCloseMessagePopup: (() -> Unit)? = null,
  onEditMessage: ((Commit.Message) -> Unit)? = null,
  onCopyMessage: ((Commit.Message) -> Unit)? = null,
  onSelectMessage: ((Commit.Message) -> Unit)? = null,
  onDeleteCommit: ((Commit.Message) -> Unit)? = null,
  onCreateBranch: ((Commit.Message) -> Unit)? = null
) {
  val newestCommit = commits.firstOrNull()
  val newestCommitId = newestCommit?.source?.id?.value
  var previousNewestCommitId by remember { mutableStateOf<String?>(null) }

  // The menu bubble keeps its slot while the exit animation plays: the last menu commit is
  // latched here and released once the popup reports through onHidden that it has fully hidden.
  var displayedMessage by remember { mutableStateOf<Commit.Message?>(null) }
  displayedMessage = focusedMessage ?: displayedMessage

  // Границы якорного пузыря в координатах окна: фокусный item сообщает их сюда, а меню-оверлей
  // живёт в корне (вне LazyColumn), поэтому переживает переработку ячейки и доигрывает fade.
  var anchorBounds by remember { mutableStateOf<IntRect?>(null) }

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

  // Подгружаем более старые коммиты, когда пользователь приближается к верху. При reverseLayout
  // верх (самый старый) — это наибольший индекс. Условия отсекают пустой первый кадр и полностью
  // видимый короткий список, чтобы ни один не вызвал ложную загрузку; distinctUntilChanged +
  // SkipNew-дебаунс на стороне вызывающего гасят повторы.
  LaunchedEffect(listState, hasCommitsHistory) {
    if (!hasCommitsHistory) {
      return@LaunchedEffect
    }
    snapshotFlow {
      val info = listState.layoutInfo
      val lastVisibleIndex = info.visibleItemsInfo.lastOrNull()?.index ?: -1
      info.visibleItemsInfo.isNotEmpty() &&
        info.totalItemsCount > info.visibleItemsInfo.size &&
        lastVisibleIndex >= info.totalItemsCount - 1 - LOAD_MORE_PREFETCH
    }
      .distinctUntilChanged()
      .filter { it }
      .collect { onLoadMore() }
  }

  Box(modifier = modifier) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
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
            val isMenuAnchor = displayedMessage?.source?.id == commit.source.id
            BubbleMessageItem(
              modifier = Modifier.animateItem(),
              bubble = commit.bubble,
              selectionEnabled = selectionEnabled,
              onClick = onMessageClick?.let { handler ->
                { handler(commit) }
              },
              onLongClick = onMessageLongClick?.let { handler ->
                { handler(commit) }
              },
              // Границы нужны только фокусному пузырю — по ним меню-оверлей встаёт на место;
              // остальные получают заглушку, реализации для них пока нет.
              onAnchorBounds = if (isMenuAnchor) {
                { bounds -> anchorBounds = bounds }
              } else {
                {}
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
      if (loadingCommitsHistory) {
        // reverseLayout: последний элемент рендерится в самом верху, над самым старым коммитом.
        item(key = "load_more_spinner") {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = AppTheme.colors.contentSecondary
            )
          }
        }
      }
    }

    // Меню-оверлей вне списка: переживает переработку ячейки-якоря (уехала под клавиатуру) и
    // доигрывает exit-fade. Позицию берёт из захваченных границ пузыря, замораживает при открытии.
    val menuCommit = displayedMessage
    val menuAnchor = anchorBounds
    if (menuCommit != null && menuAnchor != null) {
      BubbleMessagePopup(
        visible = focusedMessage != null,
        anchorBounds = menuAnchor,
        onHidden = {
          displayedMessage = null
          anchorBounds = null
        },
        onDismissRequest = { onCloseMessagePopup?.invoke() },
        onCreateBranch = onCreateBranch?.let { handler -> { handler(menuCommit) } },
        // Редактирование доступно только для своих сообщений.
        onEditMessage = onEditMessage
          ?.takeIf { menuCommit.source.isSelf }
          ?.let { handler -> { handler(menuCommit) } },
        onCopyMessage = onCopyMessage
          ?.let { handler -> { handler(menuCommit) } },
        onSelectMessage = onSelectMessage
          ?.let { handler -> { handler(menuCommit) } },
        onDeleteMessage = onDeleteCommit
          ?.let { handler -> { handler(menuCommit) } }
      )
    }
  }
}

@Composable
private fun BubbleMessagePopup(
  visible: Boolean,
  anchorBounds: IntRect,
  onHidden: () -> Unit,
  onDismissRequest: () -> Unit,
  onCreateBranch: (() -> Unit)?,
  onEditMessage: (() -> Unit)?,
  onCopyMessage: (() -> Unit)?,
  onSelectMessage: (() -> Unit)?,
  onDeleteMessage: (() -> Unit)?
) {
  fun action(action: () -> Unit): () -> Unit = {
    action.invoke()
    onDismissRequest.invoke()
  }
  Popup(
    visible = visible,
    anchorBounds = anchorBounds,
    onHidden = onHidden,
    bottomSafePadding = PopupBottomSafePadding,
    onDismissRequest = onDismissRequest,
    actions = listOfNotNull(
      onCreateBranch?.let {
        Action(
          iconRes = R.drawable.ic_git_fork_24,
          text = resRef(R.string.thread_menu_create_branch),
          onClick = action(it)
        )
      },
      onEditMessage?.let {
        Action(
          iconRes = R.drawable.ic_pencil_24,
          text = resRef(R.string.thread_menu_edit),
          onClick = action(it)
        )
      },
      onCopyMessage?.let {
        Action(
          iconRes = R.drawable.ic_copy_24,
          text = resRef(R.string.thread_menu_copy),
          onClick = action(it)
        )
      },
      onSelectMessage?.let {
        Action(
          iconRes = R.drawable.ic_select_24,
          text = resRef(R.string.thread_menu_select),
          onClick = action(it)
        )
      },
      onDeleteMessage?.let {
        Action(
          iconRes = R.drawable.ic_trash_24,
          text = resRef(R.string.thread_menu_delete),
          onClick = action(it)
        )
      }
    )
  )
}

private val PopupBottomSafePadding = 96.dp

// Запускаем следующую страницу истории за несколько элементов до самого верха, чтобы подгрузка прошла бесшовно.
private const val LOAD_MORE_PREFETCH = 5
