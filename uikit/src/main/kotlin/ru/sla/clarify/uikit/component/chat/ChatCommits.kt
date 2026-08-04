package ru.sla.clarify.uikit.component.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.component.popup.Action
import ru.sla.clarify.uikit.component.popup.Popup
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.compose.resolveTextRef
import ru.sla.resourcerefs.resRef
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import ru.sla.clarify.entity.chat.Commit as DomainCommit

@Composable
@Suppress("CyclomaticComplexMethod")
fun ChatCommits(
  listState: LazyListState,
  items: List<Commit>,
  hasHistory: Boolean,
  loadingHistory: Boolean,
  selectionEnabled: Boolean,
  focused: Commit?,
  onLoad: () -> Unit,
  onRead: (LocalDateTime) -> Unit,
  onClick: (Commit.Message) -> Unit,
  onLongClick: (Commit.Message) -> Unit,
  modifier: Modifier = Modifier,
  highlightedCommitId: DomainCommit.Id? = null,
  onHighlightHandled: (() -> Unit)? = null,
  onReply: ((Commit) -> Unit)? = null,
  onQuoteClick: ((DomainCommit.Id) -> Unit)? = null,
  onEdit: ((Commit) -> Unit)? = null,
  onCopy: ((Commit) -> Unit)? = null,
  onSelect: ((Commit) -> Unit)? = null,
  onDelete: ((Commit) -> Unit)? = null,
  onClosePopup: (() -> Unit)? = null,
  onCreateBranch: ((Commit) -> Unit)? = null
) {
  val newestCommit = items.firstOrNull()
  val newestCommitId = newestCommit?.source?.id?.value
  var previousNewestCommitId by remember { mutableStateOf<String?>(null) }

  // The menu bubble keeps its slot while the exit animation plays: the last menu commit is
  // latched here and released once the popup reports through onHidden that it has fully hidden.
  var displayedCommit by remember { mutableStateOf<Commit?>(null) }
  displayedCommit = focused ?: displayedCommit

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

  // Переход по цитате: оригинал ищем только среди загруженных коммитов — историю до него не тянем.
  // Держим подсветку заметное время и отдаём ключ обратно, чтобы она мягко погасла.
  LaunchedEffect(highlightedCommitId, items) {
    val targetId = highlightedCommitId ?: return@LaunchedEffect
    val targetIndex = items.indexOfFirst { it.source.id == targetId }
    if (targetIndex >= 0) {
      listState.animateScrollToItem(targetIndex)
      delay(HighlightHoldDuration)
    }
    onHighlightHandled?.invoke()
  }

  LaunchedEffect(listState, items) {
    snapshotFlow {
      listState.layoutInfo.visibleItemsInfo
        .asSequence()
        .filter { it.index <= items.lastIndex }
        .maxOfOrNull { items[it.index].source.timestamp }
    }
      .filterNotNull()
      .distinctUntilChanged()
      .debounce(300.milliseconds)
      .collect(onRead)
  }

  // Подгружаем более старые коммиты, когда пользователь приближается к верху. При reverseLayout
  // верх (самый старый) — это наибольший индекс. Условия отсекают пустой первый кадр и полностью
  // видимый короткий список, чтобы ни один не вызвал ложную загрузку; distinctUntilChanged +
  // SkipNew-дебаунс на стороне вызывающего гасят повторы.
  LaunchedEffect(listState, hasHistory) {
    if (!hasHistory) {
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
      .collect { onLoad() }
  }

  Box(modifier = modifier) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = listState,
      reverseLayout = true,
      verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Bottom)
    ) {
      itemsIndexed(
        items = items,
        key = { _, item -> item.source.id.value }
      ) { _, commit ->
        when (commit) {
          is Commit.Message -> {
            Message(
              modifier = Modifier.animateItem(),
              message = commit,
              selectionEnabled = selectionEnabled,
              highlighted = commit.source.id == highlightedCommitId,
              onQuoteClick = commit.replyCommit
                ?.targetId
                ?.let { targetId -> onQuoteClick?.let { handler -> { handler(targetId) } } },
              // Границы нужны только фокусному пузырю — по ним меню-оверлей встаёт на место;
              // остальным репортить нечего.
              onAnchorBounds = if (displayedCommit?.source?.id == commit.source.id) {
                { bounds -> anchorBounds = bounds }
              } else {
                null
              },
              onClick = { onClick(commit) },
              onLongClick = { onLongClick(commit) }
            )
          }
          is Commit.InviteMember -> {
            InviteMember(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              text = resolveTextRef(commit.text)
            )
          }
        }
      }
      if (loadingHistory) {
        // reverseLayout: последний элемент рендерится в самом верху, над самым старым коммитом.
        item(key = "load_more_spinner") {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = AppTheme.colors.contentPrimary
          )
        }
      }
    }
    // Меню-оверлей вне списка: переживает переработку ячейки-якоря (уехала под клавиатуру) и
    // доигрывает exit-fade. Позицию берёт из захваченных границ пузыря, замораживает при открытии.
    val popupCommit = displayedCommit
    val menuAnchor = anchorBounds
    if (popupCommit != null && menuAnchor != null) {
      CommitPopup(
        visible = focused != null,
        anchorBounds = menuAnchor,
        onDismissRequest = { onClosePopup?.invoke() },
        onHidden = {
          displayedCommit = null
          anchorBounds = null
        },
        onReplyMessage = onReply
          ?.let { handler -> { handler(popupCommit) } },
        // Редактирование доступно только для своих сообщений.
        onEditMessage = onEdit
          ?.takeIf { popupCommit.source.isSelf }
          ?.let { handler -> { handler(popupCommit) } },
        onCopyMessage = onCopy
          ?.let { handler -> { handler(popupCommit) } },
        onSelectMessage = onSelect
          ?.let { handler -> { handler(popupCommit) } },
        onDeleteMessage = onDelete
          ?.let { handler -> { handler(popupCommit) } },
        onCreateBranch = onCreateBranch
          ?.let { handler -> { handler(popupCommit) } }
      )
    }
  }
}

@Composable
private fun CommitPopup(
  visible: Boolean,
  anchorBounds: IntRect,
  onHidden: () -> Unit,
  onDismissRequest: () -> Unit,
  onCreateBranch: (() -> Unit)?,
  onReplyMessage: (() -> Unit)?,
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
      onReplyMessage?.let {
        Action(
          iconRes = R.drawable.ic_reply_24,
          text = resRef(R.string.thread_menu_reply),
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

// Сколько подсветка держится на оригинале после перехода по цитате, прежде чем начать гаснуть.
private val HighlightHoldDuration = 1200.milliseconds

// Запускаем следующую страницу истории за несколько элементов до самого верха, чтобы подгрузка прошла бесшовно.
private const val LOAD_MORE_PREFETCH = 5
