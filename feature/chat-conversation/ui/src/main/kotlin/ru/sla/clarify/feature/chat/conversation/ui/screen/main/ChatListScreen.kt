package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.conversation.ui.screen.main.components.DirectConversationItem
import ru.sla.clarify.feature.chat.conversation.ui.screen.main.components.FabActionButton
import ru.sla.clarify.feature.chat.conversation.ui.screen.main.components.rememberFabVisibility
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.uikit.component.Avatar
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.VSpacer

@Composable
fun ChatListScreen(viewModel: ChatListViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    BackHandler(
      onBack = intents.navigateBack
    )
    ChatListReadyContent(
      user = state.user,
      editModeEnabled = state.editModeEnabled,
      conversations = state.conversations,
      selectedConversationsIds = state.selectedConversationIds,
      onDirectConversation = intents.openChat,
      onConversationLongPress = intents.handleConversationLongPress,
      onShowNewChatDialog = intents.showNewChatDialog,
      onShowDeleteConfirmation = intents.showDeleteConfirmation,
      onOpenProfile = intents.openProfile
    )
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
  onShowNewChatDialog: () -> Unit,
  onShowDeleteConfirmation: () -> Unit,
  onOpenProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .systemBarsPadding()
  ) {
    val listState = rememberLazyListState()
    val fabVisibility = rememberFabVisibility(listState)

    Column(modifier = Modifier.fillMaxSize()) {
      VSpacer(8.dp)
      if (user != null) {
        Header(
          modifier = Modifier.padding(horizontal = 16.dp),
          user = user,
          onOpenProfile = onOpenProfile
        )
        VSpacer(8.dp)
      }
      if (conversations.isEmpty()) {
        ConversationEmptyState(
          modifier = Modifier.fillMaxSize()
        )
      } else {
        ConversationReadyState(
          modifier = Modifier.fillMaxSize(),
          listState = listState,
          editModeEnabled = editModeEnabled,
          conversations = conversations,
          selectedConversationsIds = selectedConversationsIds,
          onDirectConversation = onDirectConversation,
          onConversationLongPress = onConversationLongPress
        )
      }
    }
    FabActionButton(
      modifier = Modifier.align(Alignment.BottomEnd),
      visible = { fabVisibility.value },
      editModeEnabled = editModeEnabled,
      selectedConversationsIds = selectedConversationsIds,
      onShowNewChatDialog = onShowNewChatDialog,
      onShowDeleteConfirmation = onShowDeleteConfirmation
    )
  }
}

@Composable
private fun Header(
  user: User,
  onOpenProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .surface(
        shape = RoundedCornerShape(28.dp),
        backgroundColor = AppTheme.colors.cardSecondary,
        onClick = onOpenProfile
      )
      .padding(
        vertical = 12.dp,
        horizontal = 16.dp
      ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Avatar(
      size = 48.dp,
      photoUrl = user.photoUrl,
      fallbackInitial = user.email?.value?.takeIf { it.isNotBlank() } ?: "?"
    )
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      val displayName = user.displayName
      if (displayName != null) {
        Text(
          text = displayName,
          style = AppTheme.typography.title2,
          color = AppTheme.colors.contentPrimary
        )
      }
      val email = user.email
      if (email != null) {
        Text(
          text = email.value,
          style = AppTheme.typography.body3,
          color = AppTheme.colors.contentPrimary
        )
      }
    }
    Icon(
      painter = painterResource(R.drawable.ic_chevron_right_24),
      tint = AppTheme.colors.contentSecondary,
      contentDescription = null
    )
  }
}

@Composable
private fun ConversationEmptyState(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        painter = painterResource(
          if (AppTheme.colors.isLight) {
            R.drawable.ic_messages_light_160_120
          } else {
            R.drawable.ic_messages_dark_160_120
          }
        ),
        contentDescription = null
      )
      Text(
        modifier = Modifier.padding(top = 24.dp),
        text = stringResource(R.string.conversation_empty_title),
        style = AppTheme.typography.title2,
        color = AppTheme.colors.contentPrimary
      )
      Text(
        modifier = Modifier.padding(top = 8.dp),
        text = stringResource(R.string.conversation_empty_subtitle),
        style = AppTheme.typography.body2,
        color = AppTheme.colors.contentSecondary,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun ConversationReadyState(
  listState: LazyListState,
  editModeEnabled: Boolean,
  conversations: List<Conversation>,
  selectedConversationsIds: List<Conversation.Id>,
  onDirectConversation: (Peer.Id) -> Unit,
  onConversationLongPress: (Conversation.Id) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier,
    state = listState
  ) {
    items(
      items = conversations,
      key = { it.id.value }
    ) { item ->
      when (item) {
        is Conversation.Direct -> {
          DirectConversationItem(
            direct = item,
            editModeEnabled = editModeEnabled,
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
    }
    item { VSpacer(60.dp) }
  }
}
