package ru.sla.clarify.feature.chat.ui.screen.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kode.amvi.component.compose.MviComponent
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatThreadScreen(viewModel: ChatThreadViewModel) {
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
      Column(
        modifier = Modifier
          .fillMaxSize()
          .imePadding()
      ) {
        ChatThreadTopBar(
          peerUserId = state.peerUserId,
          onBack = intents.navigateBack
        )
        ChatThreadMessages(
          messages = state.messages,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        )
        HorizontalDivider()
        ChatThreadInputRow(
          value = state.inputValue,
          isSending = state.isSending,
          onValueChange = intents.inputChanged,
          onSend = intents.sendMessage,
          modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
        )
      }
    }
  }
}

@Composable
private fun ChatThreadTopBar(
  peerUserId: String,
  onBack: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(onClick = onBack) {
      Text(text = "<", style = AppTheme.typography.h2)
    }
    Text(
      modifier = Modifier.padding(start = 4.dp),
      text = peerUserId,
      style = AppTheme.typography.title1,
      fontWeight = FontWeight.SemiBold
    )
  }
  HorizontalDivider()
}

@Composable
private fun ChatThreadMessages(
  messages: List<ChatMessage>,
  modifier: Modifier = Modifier
) {
  if (messages.isEmpty()) {
    Box(
      modifier = modifier,
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "Say hi!",
        style = AppTheme.typography.body1
      )
    }
    return
  }
  val listState = rememberLazyListState()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }
  LazyColumn(
    state = listState,
    modifier = modifier.padding(horizontal = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
    contentPadding = PaddingValues(vertical = 8.dp)
  ) {
    items(
      items = messages,
      key = { it.msgId.ifEmpty { "${it.senderId}_${it.timestamp}_${it.text.hashCode()}" } }
    ) { message ->
      MessageBubble(message)
    }
  }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
  val alignment = if (message.isSelf) Alignment.End else Alignment.Start
  val bubbleColor = if (message.isSelf) AppTheme.colors.textPrimary else AppTheme.colors.textInvertPrimary
  val textColor = if (message.isSelf) AppTheme.colors.bgPrimary else AppTheme.colors.textPrimary
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = alignment
  ) {
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(BUBBLE_RADIUS_DP.dp))
        .background(bubbleColor)
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Text(
        text = message.text,
        style = AppTheme.typography.body1,
        color = textColor
      )
    }
    Text(
      text = formatTime(message.timestamp) + when (message.status) {
        ChatMessage.Status.Sending -> "  •  sending"
        ChatMessage.Status.Failed -> "  •  failed"
        ChatMessage.Status.Sent -> ""
      },
      style = AppTheme.typography.caption2,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
  }
}

@Composable
private fun ChatThreadInputRow(
  value: String,
  isSending: Boolean,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.weight(1f),
      placeholder = { Text("Message") }
    )
    Button(
      onClick = onSend,
      enabled = value.isNotBlank() && !isSending
    ) {
      Text("Send")
    }
  }
}

private fun formatTime(epochMillis: Long): String {
  if (epochMillis <= 0) return ""
  return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))
}

private const val BUBBLE_RADIUS_DP = 12
