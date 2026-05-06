package ru.sla.clarify.feature.chat.ui.screen.list

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.kode.amvi.component.compose.MviComponent
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.feature.chat.domain.entity.Conversation
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
    scaffoldState.dialogError = state.dialogError
    scaffoldState.snackbarError = state.snackbarError
    ScreenScaffold(
      state = scaffoldState,
      onDismissDialogError = intents.dismissDialogError,
      onDismissSnackbarError = intents.dismissSnackbarError
    ) {
      Scaffold(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding(),
        floatingActionButton = {
          ExtendedFloatingActionButton(
            onClick = intents.showNewChatDialog,
            text = { Text("New chat") },
            icon = {}
          )
        }
      ) { padding ->
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
          MyUserIdHeader(myUserId = state.myUserId)
          HorizontalDivider()
          ChatListBody(
            conversations = state.conversations,
            onConversationClick = intents.openChat,
            modifier = Modifier
              .fillMaxSize()
          )
        }
      }

      if (state.newChatDialogVisible) {
        NewChatDialog(
          peerId = state.peerIdInput,
          onPeerIdChange = intents.peerIdChanged,
          onConfirm = intents.confirmNewChat,
          onDismiss = intents.dismissNewChatDialog
        )
      }
    }
  }
}

@Composable
private fun MyUserIdHeader(myUserId: String?) {
  val clipboard = LocalClipboard.current
  val scope = rememberCoroutineScope()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = "Your userId",
        style = AppTheme.typography.caption2
      )
      Text(
        text = myUserId ?: "(not signed in)",
        style = AppTheme.typography.title3,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (myUserId != null) {
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
private fun ChatListBody(
  conversations: List<Conversation>,
  onConversationClick: (String) -> Unit,
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
      key = { it.peerUserId }
    ) { conversation ->
      ConversationRow(
        conversation = conversation,
        onClick = { onConversationClick(conversation.peerUserId) }
      )
      HorizontalDivider()
    }
  }
}

@Composable
private fun ConversationRow(
  conversation: Conversation,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(AppTheme.colors.textInvertPrimary),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = (conversation.peerNickname ?: conversation.peerUserId).take(1).uppercase(),
        style = AppTheme.typography.button,
        color = AppTheme.colors.textPrimary
      )
    }
    Column(
      modifier = Modifier
        .padding(start = 12.dp)
        .weight(1f)
    ) {
      Text(
        text = conversation.peerNickname?.takeIf { it.isNotBlank() } ?: conversation.peerUserId,
        style = AppTheme.typography.button,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      val preview = conversation.lastMessage?.takeIf { it.isNotBlank() } ?: "(no messages yet)"
      Text(
        text = preview,
        style = AppTheme.typography.body2,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (conversation.unreadCount > 0) {
      Box(
        modifier = Modifier
          .padding(start = 8.dp)
          .size(24.dp)
          .clip(CircleShape)
          .background(AppTheme.colors.textPrimary),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = conversation.unreadCount.coerceAtMost(MAX_UNREAD_BADGE).toString(),
          style = AppTheme.typography.body2,
          color = AppTheme.colors.bgPrimary
        )
      }
    }
  }
}

@Composable
private fun NewChatDialog(
  peerId: String,
  onPeerIdChange: (String) -> Unit,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New chat") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Enter the peer userId to chat with:")
        OutlinedTextField(
          value = peerId,
          onValueChange = onPeerIdChange,
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        enabled = peerId.isNotBlank()
      ) {
        Text("Start")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

private const val MAX_UNREAD_BADGE = 99
