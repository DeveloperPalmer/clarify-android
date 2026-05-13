package ru.sla.clarify.feature.chat.ui.screen.thread

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.core.ui.text.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun ChatThreadScreen(viewModel: ChatThreadViewModel) {
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
      ChatThreadReadyContent(
        peerId = state.peerId,
        messages = state.messages,
        isSending = state.isSending,
        onBack = intents.navigateBack,
        onSend = intents.sendMessage,
        onChronology = intents.openChronology
      )
    }
  }
}

@Composable
internal fun ChatThreadReadyContent(
  peerId: String,
  messages: List<ChatMessage>,
  isSending: Boolean,
  onBack: () -> Unit,
  onChronology: () -> Unit,
  onSend: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .imePadding()
  ) {
    ChatThreadTopBar(
      peerId = peerId,
      onBack = onBack,
      onChronology = onChronology
    )
    if (messages.isEmpty()) {
      TreadEmptyState(
        modifier = Modifier
          .fillMaxSize()
      )
    } else {
      ChatThreadMessages(
        messages = messages,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
    }
    HorizontalDivider()
    ChatThreadInputRow(
      isSending = isSending,
      onSend = onSend,
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
    )
  }
}

@Composable
private fun ChatThreadTopBar(
  peerId: String,
  onBack: () -> Unit,
  onChronology: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    IconButton(onClick = onBack) {
      Text(
        text = "<",
        style = AppTheme.typography.h2
      )
    }
    Text(
      modifier = Modifier
        .weight(1f)
        .padding(start = 4.dp),
      text = peerId,
      style = AppTheme.typography.title1,
      fontWeight = FontWeight.SemiBold
    )
    IconButton(onClick = onChronology) {
      Text(
        text = "H",
        style = AppTheme.typography.h2
      )
    }
  }
  HorizontalDivider()
}

@Composable
private fun ChatThreadMessages(
  messages: List<ChatMessage>,
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(0)
    }
  }
  LazyColumn(
    modifier = modifier,
    state = listState,
    reverseLayout = true,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    contentPadding = PaddingValues(8.dp)
  ) {
    items(
      items = messages,
      key = { it.id.value.ifEmpty { "${it.senderId}_${it.timestamp}_${it.text.hashCode()}" } }
    ) { message ->
      MessageBubble(message)
    }
  }
}

@Composable
private fun TreadEmptyState(modifier: Modifier = Modifier) {
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

@Composable
private fun MessageBubble(message: ChatMessage) {
  val alignment = if (message.isSelf) Alignment.End else Alignment.Start
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = alignment
  ) {
    Box(
      modifier = Modifier
        .surface(
          shape = AppTheme.shapes.round12,
          backgroundColor = if (message.isSelf) {
            AppTheme.colors.backgroundSecondary
          } else {
            AppTheme.colors.textPrimary
          }
        )
        .padding(
          vertical = 8.dp,
          horizontal = 12.dp
        )
    ) {
      Text(
        text = message.text,
        style = AppTheme.typography.body1,
        color = if (message.isSelf) {
          AppTheme.colors.textPrimary
        } else {
          AppTheme.colors.backgroundSecondary
        }
      )
    }
    Text(
      text = message.timestamp.format(TIME_FORMATTER_HOUR_MINUTE) + when (message.status) {
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
  isSending: Boolean,
  onSend: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var inputValue by rememberSaveable { mutableStateOf("") }
  Row(
    modifier = modifier
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    OutlinedTextField(
      value = inputValue,
      onValueChange = { inputValue = it },
      modifier = Modifier.weight(1f),
      placeholder = { Text("Message") }
    )
    Button(
      onClick = {
        val text = inputValue.trim()
        if (text.isNotEmpty()) {
          onSend(text)
          inputValue = ""
        }
      },
      enabled = inputValue.isNotBlank() && !isSending
    ) {
      Text("Send")
    }
  }
}
