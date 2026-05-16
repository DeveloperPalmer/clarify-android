package ru.sla.clarify.feature.chat.ui.screen.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import java.time.Instant
import java.time.ZoneId

@Preview
@Composable
private fun ChatThreadReadyContentPreview(
  @PreviewParameter(ChatThreadPreviewProvider::class) state: ChatThreadPreviewState
) {
  AppTheme(currentTheme = state.theme) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(AppTheme.colors.backgroundPrimary)
    ) {
      ChatThreadReadyContent(
        peerId = state.peerId,
        messages = state.messages,
        isSending = state.isSending,
        onBack = {},
        onSend = {}
      )
    }
  }
}

internal data class ChatThreadPreviewState(
  val label: String,
  val theme: ColorTheme,
  val peerId: String,
  val messages: List<ChatMessage>,
  val isSending: Boolean
) {
  // Compose tooling использует toString() как заголовок для каждого варианта,
  // поэтому отдаём короткий label вместо длинного дампа data class'а.
  override fun toString(): String = label
}

internal class ChatThreadPreviewProvider : PreviewParameterProvider<ChatThreadPreviewState> {
  override val values: Sequence<ChatThreadPreviewState> = sequenceOf(
    ChatThreadPreviewState(
      label = "Light",
      theme = ColorTheme.Light,
      peerId = "Алиса",
      messages = sampleMessages(),
      isSending = false
    ),
    ChatThreadPreviewState(
      label = "Dark",
      theme = ColorTheme.Dark,
      peerId = "Алиса",
      messages = sampleMessages(),
      isSending = true
    ),
    ChatThreadPreviewState(
      label = "Empty",
      theme = ColorTheme.Light,
      peerId = "Боб",
      messages = emptyList(),
      isSending = false
    ),
    ChatThreadPreviewState(
      label = "Long conversation",
      theme = ColorTheme.Light,
      peerId = "Кейт",
      messages = longSampleMessages(),
      isSending = false
    )
  )
}

private fun sampleMessages(): List<ChatMessage> {
  val now = Instant.ofEpochSecond(PREVIEW_NOW_EPOCH_MILLIS)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()
  return listOf(
    ChatMessage(
      id = ChatMessage.Id("m-1"),
      parentId = ChatMessage.Id("m-1"),
      peerId = "Алиса",
      senderId = "alice",
      text = "Привет! Как дела?",
      timestamp = now.minusSeconds(MIN_30),
      isSelf = false,
      colorHex = "",
      status = ChatMessage.Status.Sent
    ),
    ChatMessage(
      id = ChatMessage.Id("m-2"),
      parentId = ChatMessage.Id("m-1"),
      peerId = "Алиса",
      senderId = "me",
      text = "Привет! Всё отлично, спасибо.",
      timestamp = now.minusSeconds(MIN_28),
      isSelf = true,
      colorHex = "",
      status = ChatMessage.Status.Sent
    ),
    ChatMessage(
      id = ChatMessage.Id("m-3"),
      parentId = ChatMessage.Id("m-3"),
      peerId = "Алиса",
      senderId = "alice",
      text = "Чем сейчас занимаешься?",
      timestamp = now.minusSeconds(MIN_5),
      isSelf = false,
      colorHex = "",
      status = ChatMessage.Status.Sent
    ),
    ChatMessage(
      id = ChatMessage.Id("m-4"),
      parentId = ChatMessage.Id("m-4"),
      peerId = "Алиса",
      senderId = "me",
      text = "Делаю превью экранов в Compose. Подбираю пастельные тона.",
      timestamp = now.minusSeconds(MIN_2),
      isSelf = true,
      colorHex = "",
      status = ChatMessage.Status.Sending
    ),
    ChatMessage(
      id = ChatMessage.Id("m-5"),
      parentId = ChatMessage.Id("m-5"),
      peerId = "Алиса",
      senderId = "me",
      text = "Кажется, отправка зависла…",
      timestamp = now.minusSeconds(SEC_30),
      isSelf = true,
      colorHex = "",
      status = ChatMessage.Status.Failed
    )
  )
}

private fun longSampleMessages(): List<ChatMessage> {
  val base = sampleMessages()
  val now = Instant.ofEpochSecond(PREVIEW_NOW_EPOCH_MILLIS)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()
  val tail = listOf(
    ChatMessage(
      id = ChatMessage.Id("m-6"),
      parentId = ChatMessage.Id("m-6"),
      peerId = "Кейт",
      senderId = "kate",
      text = "Покажи скрин, когда будет готово",
      timestamp = now.minusSeconds(SEC_20),
      isSelf = false,
      colorHex = "",
      status = ChatMessage.Status.Sent
    ),
    ChatMessage(
      id = ChatMessage.Id("m-7"),
      parentId = ChatMessage.Id("m-7"),
      peerId = "Кейт",
      senderId = "me",
      text = "Конечно, скоро пришлю",
      timestamp = now.minusSeconds(SEC_10),
      isSelf = true,
      colorHex = "",
      status = ChatMessage.Status.Sent
    ),
    ChatMessage(
      id = ChatMessage.Id("m-8"),
      parentId = ChatMessage.Id("m-8"),
      peerId = "Кейт",
      senderId = "kate",
      text = "Спасибо!",
      timestamp = now.minusSeconds(SEC_5),
      isSelf = false,
      colorHex = "",
      status = ChatMessage.Status.Sent
    )
  )
  return base + tail
}

// Фиксированная "точка отсчёта" — превью не должно зависеть от текущего времени,
// иначе скриншоты будут флакать в дизайнерских ревью.
private const val PREVIEW_NOW_EPOCH_MILLIS = 1_715_000_000_000L
private const val SEC_5 = 5_000L
private const val SEC_10 = 10_000L
private const val SEC_20 = 20_000L
private const val SEC_30 = 30_000L
private const val MIN_2 = 2 * 60_000L
private const val MIN_5 = 5 * 60_000L
private const val MIN_28 = 28 * 60_000L
private const val MIN_30 = 30 * 60_000L
