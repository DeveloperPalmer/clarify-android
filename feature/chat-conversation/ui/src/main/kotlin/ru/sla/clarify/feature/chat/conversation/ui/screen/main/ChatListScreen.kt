package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.event.captureDropdownMenuAnchor
import ru.sla.clarify.core.ui.event.rememberDropdownMenuAnchorScope
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.uikit.component.Avatar
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
        user = state.user,
        editModeEnabled = state.editModeEnabled,
        conversations = state.conversations,
        selectedConversationsIds = state.selectedConversationIds,
        onDirectConversation = intents.openChat,
        onConversationLongPress = intents.handleConversationLongPress,
        onShowNewChatDialog = intents.showNewChatDialog,
        onOpenSettings = intents.openSettings,
        onOpenProfile = intents.openProfile
      )
    }
  }
}

@Composable
internal fun ChatListReadyContent(
  user: User?,
  conversations: List<Conversation>,
  selectedConversationsIds: List<Conversation.Id>,
  editModeEnabled: Boolean,
  onDirectConversation: (Peer.Id) -> Unit,
  onConversationLongPress: (Conversation.Id) -> Unit,
  onOpenSettings: () -> Unit,
  onShowNewChatDialog: () -> Unit,
  onOpenProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .systemBarsPadding()
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Header(
        user = user,
        editModeEnabled = editModeEnabled,
        onOpenSettings = onOpenSettings,
        onOpenProfile = onOpenProfile
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
      text = { Text(stringResource(R.string.conversation_new_chat_button)) },
      icon = {}
    )
  }
}

@Composable
private fun Header(
  user: User?,
  editModeEnabled: Boolean,
  onOpenSettings: () -> Unit,
  onOpenProfile: () -> Unit
) {
  val email = user?.email
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()
  val dropdownMenuAnchorScope = rememberDropdownMenuAnchorScope()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Avatar(
      modifier = Modifier
        .clip(CircleShape)
        .clickable(onClick = onOpenProfile),
      photoUrl = user?.photoUrl,
      fallbackInitial = email?.value?.takeIf { it.isNotBlank() } ?: "?"
    )
    Column(
      modifier = Modifier
        .padding(start = 12.dp)
        .weight(1f)
    ) {
      Text(
        text = stringResource(R.string.conversation_header_email_label),
        style = AppTheme.typography.caption2,
        color = AppTheme.colors.textPrimary
      )
      Text(
        text = email?.value ?: stringResource(R.string.conversation_header_not_signed_in),
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
        Text(stringResource(R.string.conversation_header_settings_button))
      }
    } else if (email != null) {
      TextButton(
        onClick = {
          scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("", email.value)))
          }
        }
      ) {
        Text(stringResource(R.string.conversation_header_copy_button))
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
        text = stringResource(R.string.conversation_empty_state),
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
      Avatar(
        photoUrl = direct.peer.photoUrl,
        fallbackInitial = direct.peer.displayName
          ?.takeIf { it.isNotBlank() }
          ?: direct.peer.id.value,
        highlighted = selected
      )
      Column(
        modifier = Modifier
          .padding(start = 12.dp)
          .weight(1f)
      ) {
        Text(
          text = direct.peer.displayName
            ?.takeIf { it.isNotBlank() }
            ?: direct.peer.id.value,
          style = AppTheme.typography.button,
          color = AppTheme.colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = direct.lastMessage
            ?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.conversation_no_messages_preview),
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
