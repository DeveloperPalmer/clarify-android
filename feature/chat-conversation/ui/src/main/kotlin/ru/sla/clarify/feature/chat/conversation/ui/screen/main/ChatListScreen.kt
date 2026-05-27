package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kode.amvi.component.compose.rememberViewIntents
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.uikit.component.Avatar
import ru.sla.clarify.uikit.component.button.ErrorButton
import ru.sla.clarify.uikit.component.button.TertiaryButton
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.VSpacer

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
        onShowDeleteConfirmation = intents.showDeleteConfirmation,
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
    Column(modifier = Modifier.fillMaxSize()) {
      VSpacer(8.dp)
      if (user != null) {
        Header(
          modifier = Modifier.padding(horizontal = 16.dp),
          user = user,
          onOpenProfile = onOpenProfile
        )
      }
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
      modifier = Modifier.align(Alignment.BottomEnd),
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
          style = AppTheme.typography.caption,
          color = AppTheme.colors.contentPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = direct.lastMessage
            ?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.conversation_no_messages_preview),
          style = AppTheme.typography.body2,
          color = AppTheme.colors.contentPrimary,
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
            .background(AppTheme.colors.cardPrimary),
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

@Composable
private fun ExtendedFloatingActionButton(
  editModeEnabled: Boolean,
  selectedConversationsIds: List<Conversation.Id>,
  onShowNewChatDialog: () -> Unit,
  onShowDeleteConfirmation: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(bottom = 16.dp, end = 16.dp),
    contentAlignment = Alignment.BottomEnd
  ) {
    AnimatedVisibility(
      visible = !editModeEnabled,
      enter = fadeIn() + scaleIn(),
      exit = fadeOut() + scaleOut()
    ) {
      TertiaryButton(
        onClick = onShowNewChatDialog,
        text = stringResource(R.string.conversation_new_chat_button)
      )
    }
    AnimatedVisibility(
      visible = editModeEnabled && selectedConversationsIds.isNotEmpty(),
      enter = fadeIn() + scaleIn(),
      exit = fadeOut() + scaleOut()
    ) {
      ErrorButton(
        onClick = onShowDeleteConfirmation,
        text = stringResource(R.string.delete)
      )
    }
  }
}

private const val MAX_UNREAD_BADGE = 99
