package ru.sla.clarify.feature.chat.conversation.ui.screen.main.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.entity.chat.Conversation
import ru.sla.clarify.uikit.component.UnreadCountBadge
import ru.sla.clarify.uikit.component.avatar.Avatar
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.compose.resolveTextRef

@Composable
internal fun DirectConversationItem(
  direct: Conversation.Direct,
  editModeEnabled: Boolean,
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
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box {
        Avatar(
          size = 40.dp,
          photoUrl = direct.peer.photoUrl,
          fallback = direct.peer.displayName
        )
        DoneBadge(
          modifier = Modifier
            .size(18.dp)
            .align(Alignment.BottomEnd),
          visible = editModeEnabled && selected
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = direct.peer.displayName,
          style = AppTheme.typography.title2Bold,
          color = AppTheme.colors.contentPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        val lastCommit = direct.lastCommit
        if (lastCommit != null) {
          Text(
            text = lastCommit,
            style = AppTheme.typography.body3,
            color = AppTheme.colors.contentPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
      TrailingBlock(
        date = direct.lastCommitAt?.let { resolveTextRef(it) },
        unreadCount = direct.unreadCount
      )
    }
  }
}

@Composable
internal fun DoneBadge(
  visible: Boolean,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    modifier = modifier,
    visible = visible
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .clip(CircleShape)
        .background(AppTheme.colors.cardPrimary)
        .padding(2.dp)
        .clip(CircleShape)
        .background(AppTheme.colors.contentAccentPrimary),
      contentAlignment = Alignment.Center
    ) {
      Image(
        modifier = Modifier.size(10.dp),
        painter = painterResource(R.drawable.ic_check_16),
        contentDescription = null,
        colorFilter = ColorFilter.tint(AppTheme.colors.contentAccentSecondary)
      )
    }
  }
}

@Composable
internal fun TrailingBlock(
  date: String?,
  unreadCount: Long,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.animateContentSize(),
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    if (date != null) {
      Text(
        text = date,
        style = AppTheme.typography.label3,
        color = AppTheme.colors.contentAccentPrimary
      )
    }
    UnreadCountBadge(
      unreadCount = unreadCount
    )
  }
}
