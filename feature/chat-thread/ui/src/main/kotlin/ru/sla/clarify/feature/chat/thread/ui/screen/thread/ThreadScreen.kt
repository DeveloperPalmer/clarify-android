package ru.sla.clarify.feature.chat.thread.ui.screen.thread

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.core.ui.text.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun ThreadScreen(viewModel: ThreadViewModel) {
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
      ThreadReadyContent(
        peerId = state.peerId,
        commits = state.commits,
        isSending = state.isSending,
        onBack = intents.navigateBack,
        onSend = intents.sendCommit
      )
    }
  }
}

@Composable
internal fun ThreadReadyContent(
  peerId: Peer.Id,
  commits: List<Commit>,
  isSending: Boolean,
  onBack: () -> Unit,
  onSend: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .imePadding()
  ) {
    TopBar(
      peerId = peerId,
      onBack = onBack
    )
    if (commits.isEmpty()) {
      TreadEmptyState(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
    } else {
      Commits(
        commits = commits,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
    }
    HorizontalDivider()
    InputRow(
      isSending = isSending,
      onSend = onSend,
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
    )
  }
}

@Composable
private fun TopBar(
  peerId: Peer.Id,
  onBack: () -> Unit
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
      text = peerId.value,
      style = AppTheme.typography.title1,
      fontWeight = FontWeight.SemiBold
    )
  }
  HorizontalDivider()
}

@Composable
private fun Commits(
  commits: List<Commit>,
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()
  LaunchedEffect(commits.size) {
    if (commits.isNotEmpty()) {
      listState.animateScrollToItem(0)
    }
  }
  LazyColumn(
    modifier = modifier,
    state = listState,
    reverseLayout = true,
    verticalArrangement = Arrangement.spacedBy(
      space = 4.dp,
      alignment = Alignment.Bottom
    ),
    contentPadding = PaddingValues(8.dp)
  ) {
    items(
      items = commits,
      key = { it.id.value.ifEmpty { "${it.senderId.value}_${it.timestamp}_${it.text.hashCode()}" } }
    ) { commit ->
      CommitBubble(commit)
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
private fun CommitBubble(commit: Commit) {
  val alignment = if (commit.isSelf) Alignment.End else Alignment.Start
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = alignment
  ) {
    Box(
      modifier = Modifier
        .surface(
          shape = AppTheme.shapes.round12,
          backgroundColor = Color(commit.colorHex.toColorInt())
        )
        .padding(
          vertical = 8.dp,
          horizontal = 12.dp
        )
    ) {
      Text(
        text = commit.text,
        style = AppTheme.typography.body1,
        color = AppTheme.colors.textPrimary
      )
    }
    Text(
      text = commit.timestamp.format(TIME_FORMATTER_HOUR_MINUTE) + when (commit.status) {
        Commit.Status.Sending -> "  •  sending"
        Commit.Status.Failed -> "  •  failed"
        Commit.Status.Sent -> ""
      },
      color = AppTheme.colors.textPrimary,
      style = AppTheme.typography.caption2,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
  }
}

@Composable
private fun InputRow(
  isSending: Boolean,
  onSend: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    var inputValue by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
      modifier = Modifier.weight(1f),
      value = inputValue,
      onValueChange = { inputValue = it },
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
