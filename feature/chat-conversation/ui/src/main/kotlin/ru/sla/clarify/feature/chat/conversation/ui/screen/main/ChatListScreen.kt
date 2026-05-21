package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.core.ui.event.captureDropdownMenuAnchor
import ru.sla.clarify.core.ui.event.rememberDropdownMenuAnchorScope
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun ChatListScreen(viewModel: ChatListViewModel) {
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
      ChatListReadyContent(
        myUserId = state.userId,
        editModeEnabled = state.editModeEnabled,
        conversations = state.conversations,
        selectedConversationsIds = state.selectedConversationIds,
        onDirectConversation = intents.openChat,
        onConversationLongPress = intents.handleConversationLongPress,
        onShowNewChatDialog = intents.showNewChatDialog,
        onOpenSettings = intents.openSettings
      )
    }
  }
}

@Composable
internal fun ChatListReadyContent(
  myUserId: String?,
  conversations: List<Conversation>,
  selectedConversationsIds: List<Conversation.Id>,
  editModeEnabled: Boolean,
  onDirectConversation: (Peer.Id) -> Unit,
  onConversationLongPress: (Conversation.Id) -> Unit,
  onOpenSettings: () -> Unit,
  onShowNewChatDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .systemBarsPadding()
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Header(
        myUserId = myUserId,
        editModeEnabled = editModeEnabled,
        onOpenSettings = onOpenSettings
      )
      HorizontalDivider()
      Conversations(
        modifier = Modifier.fillMaxSize(),
        editModeEnabled = editModeEnabled,
        conversations = conversations,
        selectedConversationsIds = selectedConversationsIds,
        onDirectConversation = onDirectConversation,
        onConversationLongPress = onConversationLongPress
      )
    }
    ExtendedFloatingActionButton(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(bottom = 16.dp, end = 16.dp),
      onClick = onShowNewChatDialog,
      text = { Text("New chat") },
      icon = {}
    )
  }
}

@Composable
private fun Header(
  myUserId: String?,
  editModeEnabled: Boolean,
  onOpenSettings: () -> Unit
) {
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()
  val dropdownMenuAnchorScope = rememberDropdownMenuAnchorScope()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "Your userId",
        style = AppTheme.typography.caption2,
        color = AppTheme.colors.textPrimary
      )
      Text(
        text = myUserId ?: "(not signed in)",
        style = AppTheme.typography.title3,
        color = AppTheme.colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (editModeEnabled) {
      TextButton(
        modifier = Modifier.captureDropdownMenuAnchor(dropdownMenuAnchorScope),
        onClick = {
          dropdownMenuAnchorScope.anchor()
          onOpenSettings()
        }
      ) {
        Text("Settings")
      }
    } else if (myUserId != null) {
      TextButton(
        onClick = {
          scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("", myUserId)))
          }
        }
      ) {
        Text("Copy")
      }
    }
  }
}

@Composable
private fun Conversations(
  editModeEnabled: Boolean,
  conversations: List<Conversation>,
  selectedConversationsIds: List<Conversation.Id>,
  onDirectConversation: (Peer.Id) -> Unit,
  onConversationLongPress: (Conversation.Id) -> Unit,
  modifier: Modifier = Modifier
) {
  if (conversations.isEmpty()) {
    Box(
      modifier = modifier,
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "No conversations yet. Tap \"New chat\" to start one.",
        style = AppTheme.typography.body2
      )
    }
    return
  }
  LazyColumn(modifier = modifier) {
    items(
      items = conversations,
      key = { it.id.value }
    ) { item ->
      when (item) {
        is Conversation.Direct -> {
          DirectConversationItem(
            direct = item,
            selected = selectedConversationsIds.contains(item.id),
            onClick = {
              if (editModeEnabled) {
                onConversationLongPress(item.id)
              } else {
                onDirectConversation(item.peer.id)
              }
            },
            onLongClick = {
              onConversationLongPress(item.id)
            }
          )
        }
      }
      HorizontalDivider()
    }
  }
}

@Composable
private fun DirectConversationItem(
  direct: Conversation.Direct,
  selected: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit
) {
  Box(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
          onClick = onClick,
          onLongClick = onLongClick
        )
        .padding(
          vertical = 12.dp,
          horizontal = 16.dp
        ),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(
            if (selected) AppTheme.colors.errorPrimary else AppTheme.colors.textPrimary
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = direct.peer.id.value.take(1).uppercase(),
          style = AppTheme.typography.button,
          color = AppTheme.colors.backgroundSecondary
        )
      }
      Column(
        modifier = Modifier
          .padding(start = 12.dp)
          .weight(1f)
      ) {
        Text(
          text = direct.peer.id.value,
          style = AppTheme.typography.button,
          color = AppTheme.colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        val preview = direct.lastMessage?.takeIf { it.isNotBlank() } ?: "(no messages yet)"
        Text(
          text = preview,
          style = AppTheme.typography.body2,
          color = AppTheme.colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      if (direct.unreadCount > 0) {
        Box(
          modifier = Modifier
            .padding(start = 8.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(AppTheme.colors.backgroundSecondary),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = direct.unreadCount.coerceAtMost(MAX_UNREAD_BADGE.toLong()).toString(),
            style = AppTheme.typography.body2,
            color = AppTheme.colors.backgroundPrimary
          )
        }
      }
    }
  }
}

private const val MAX_UNREAD_BADGE = 99
