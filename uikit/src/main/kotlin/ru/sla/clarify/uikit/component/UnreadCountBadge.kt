package ru.sla.clarify.uikit.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun UnreadCountBadge(
  unreadCount: Long,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    modifier = modifier,
    visible = unreadCount > 0
  ) {
    Box(
      modifier = Modifier
        .size(20.dp)
        .clip(CircleShape)
        .background(AppTheme.colors.contentAccentPrimary),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = unreadCount.coerceAtMost(MAX_UNREAD_BADGE).toString(),
        style = AppTheme.typography.label3,
        color = AppTheme.colors.contentAccentSecondary
      )
    }
  }
}

private const val MAX_UNREAD_BADGE = 99L
