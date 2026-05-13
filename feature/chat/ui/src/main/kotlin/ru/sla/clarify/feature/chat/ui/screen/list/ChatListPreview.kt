package ru.sla.clarify.feature.chat.ui.screen.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ru.sla.clarify.feature.chat.domain.entity.Conversation
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

@Preview
@Composable
private fun ChatListReadyContentPreview(
  @PreviewParameter(ChatListPreviewProvider::class) state: ChatListPreviewState
) {
  AppTheme(currentTheme = state.theme) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(AppTheme.colors.backgroundPrimary)
    ) {
      ChatListReadyContent(
        myUserId = state.myUserId,
        conversations = state.conversations,
        newChatDialogVisible = state.newChatDialogVisible,
        peerIdInput = state.peerIdInput,
        onConversationClick = {},
        onConversationLongPress = {},
        onDismissDeleteMenu = {},
        onShowNewChatDialog = {},
        onDismissNewChatDialog = {},
        onPeerIdChange = {},
        conversationDeletionId = null,
        onDeleteConfirmation = {},
        onConfirmNewChat = {}
      )
    }
  }
}

internal data class ChatListPreviewState(
  val label: String,
  val theme: ColorTheme,
  val myUserId: String?,
  val conversations: List<Conversation>,
  val newChatDialogVisible: Boolean,
  val peerIdInput: String,
  val pendingDelete: Conversation? = null,
  val deleteConfirmVisible: Boolean = false
) {
  // Compose tooling использует toString() как заголовок для каждого варианта,
  // поэтому отдаём короткий label вместо длинного дампа data class'а.
  override fun toString(): String = label
}

internal class ChatListPreviewProvider : PreviewParameterProvider<ChatListPreviewState> {
  override val values: Sequence<ChatListPreviewState> = sequenceOf(
    ChatListPreviewState(
      label = "Light",
      theme = ColorTheme.Light,
      myUserId = "user-12345",
      conversations = sampleConversations(),
      newChatDialogVisible = false,
      peerIdInput = ""
    ),
    ChatListPreviewState(
      label = "Dark",
      theme = ColorTheme.Dark,
      myUserId = "user-12345",
      conversations = sampleConversations(),
      newChatDialogVisible = false,
      peerIdInput = ""
    ),
    ChatListPreviewState(
      label = "Empty",
      theme = ColorTheme.Light,
      myUserId = "user-12345",
      conversations = emptyList(),
      newChatDialogVisible = false,
      peerIdInput = ""
    ),
    ChatListPreviewState(
      label = "Not signed in",
      theme = ColorTheme.Light,
      myUserId = null,
      conversations = emptyList(),
      newChatDialogVisible = false,
      peerIdInput = ""
    ),
    ChatListPreviewState(
      label = "New chat dialog",
      theme = ColorTheme.Light,
      myUserId = "user-12345",
      conversations = sampleConversations(),
      newChatDialogVisible = true,
      peerIdInput = "alice"
    ),
    ChatListPreviewState(
      label = "Long list",
      theme = ColorTheme.Light,
      myUserId = "user-12345",
      conversations = longSampleConversations(),
      newChatDialogVisible = false,
      peerIdInput = ""
    ),
    ChatListPreviewState(
      label = "Delete confirmation",
      theme = ColorTheme.Light,
      myUserId = "user-12345",
      conversations = sampleConversations(),
      newChatDialogVisible = false,
      peerIdInput = "",
      pendingDelete = sampleConversations().first(),
      deleteConfirmVisible = true
    )
  )
}

private fun sampleConversations(): List<Conversation> = listOf(
  Conversation(
    id = Conversation.Id("c-1"),
    peer = Conversation.Peer(id = "alice", name = "Алиса", faceUrl = null),
    lastMessage = "Привет! Как дела?",
    lastMessageTimestamp = PREVIEW_NOW_EPOCH_MILLIS - MIN_5,
    unreadCount = 2
  ),
  Conversation(
    id = Conversation.Id("c-2"),
    peer = Conversation.Peer(id = "bob", name = "Боб", faceUrl = null),
    lastMessage = null,
    lastMessageTimestamp = PREVIEW_NOW_EPOCH_MILLIS - HOUR_1,
    unreadCount = 0
  ),
  Conversation(
    id = Conversation.Id("c-3"),
    peer = Conversation.Peer(id = "kate", name = "Кейт", faceUrl = null),
    lastMessage = "Готовлю апдейт, скоро пришлю длинный текст для проверки эллипсиса",
    lastMessageTimestamp = PREVIEW_NOW_EPOCH_MILLIS - DAY_1,
    unreadCount = 105
  )
)

private fun longSampleConversations(): List<Conversation> {
  val base = sampleConversations()
  val extra = (1..12).map { idx ->
    Conversation(
      id = Conversation.Id("c-extra-$idx"),
      peer = Conversation.Peer(
        id = "peer-$idx",
        name = "Контакт №$idx",
        faceUrl = null
      ),
      lastMessage = "Сообщение из истории #$idx",
      lastMessageTimestamp = PREVIEW_NOW_EPOCH_MILLIS - idx * HOUR_1,
      unreadCount = (idx % 4).toLong()
    )
  }
  return base + extra
}

// Фиксированная "точка отсчёта" — превью не должно зависеть от текущего времени,
// иначе скриншоты будут флакать в дизайнерских ревью.
private const val PREVIEW_NOW_EPOCH_MILLIS = 1_715_000_000_000L
private const val MIN_5 = 5 * 60_000L
private const val HOUR_1 = 60 * 60_000L
private const val DAY_1 = 24 * HOUR_1
