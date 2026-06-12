package ru.sla.clarify.feature.chat.conversation.ui.screen.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.uikit.component.button.ButtonStyle
import ru.sla.clarify.uikit.component.button.PrimaryButton
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
internal fun FabActionButton(
  visible: () -> Boolean,
  editModeEnabled: Boolean,
  selectedConversationsIds: List<Conversation.Id>,
  onShowNewChatDialog: () -> Unit,
  onShowDeleteConfirmation: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bottomCenter = TransformOrigin(
    pivotFractionX = 0.5f,
    pivotFractionY = 1f
  )
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(bottom = 16.dp, end = 16.dp),
    contentAlignment = Alignment.BottomEnd
  ) {
    AnimatedVisibility(
      visible = visible(),
      enter = slideInVertically { it } + scaleIn(transformOrigin = bottomCenter),
      exit = slideOutVertically { it } + scaleOut(transformOrigin = bottomCenter)
    ) {
      Box(contentAlignment = Alignment.BottomEnd) {
        AnimatedVisibility(
          visible = !editModeEnabled,
          enter = fadeIn() + scaleIn(),
          exit = fadeOut() + scaleOut()
        ) {
          NewChatFab(onClick = onShowNewChatDialog)
        }
        AnimatedVisibility(
          visible = editModeEnabled && selectedConversationsIds.isNotEmpty(),
          enter = fadeIn() + scaleIn(),
          exit = fadeOut() + scaleOut()
        ) {
          PrimaryButton(
            onClick = onShowDeleteConfirmation,
            text = stringResource(R.string.delete),
            style = ButtonStyle.Error,
            showElevation = true
          )
        }
      }
    }
  }
}

@Composable
private fun NewChatFab(onClick: () -> Unit) {
  val description = stringResource(R.string.conversation_new_chat_button)
  Box(
    modifier = Modifier
      .size(56.dp)
      .surface(
        backgroundColor = AppTheme.colors.contentAccentPrimary,
        shape = CircleShape,
        elevation = 6.dp,
        onClick = onClick
      )
      .semantics { contentDescription = description },
    contentAlignment = Alignment.Center
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_plus_24),
      contentDescription = null,
      tint = AppTheme.colors.contentAccentSecondary
    )
  }
}

@Composable
internal fun rememberFabVisibility(listState: LazyListState): State<Boolean> = remember(listState) {
  var previousIndex = listState.firstVisibleItemIndex
  var previousOffset = listState.firstVisibleItemScrollOffset
  var visible = true
  derivedStateOf {
    val currentIndex = listState.firstVisibleItemIndex
    val currentOffset = listState.firstVisibleItemScrollOffset
    if (!listState.canScrollForward) {
      visible = true
    } else if (currentIndex != previousIndex) {
      visible = currentIndex < previousIndex
    } else if (currentOffset != previousOffset) {
      visible = currentOffset < previousOffset
    }
    previousIndex = currentIndex
    previousOffset = currentOffset
    visible
  }
}
