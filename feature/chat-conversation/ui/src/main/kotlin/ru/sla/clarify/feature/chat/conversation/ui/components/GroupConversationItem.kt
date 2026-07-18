package ru.sla.clarify.feature.chat.conversation.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.component.avatar.Avatar
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

@Composable
internal fun GroupConversationItem(
  groupName: String,
  lastSenderName: String?,
  lastCommit: String?,
  lastCommitAt: String?,
  unreadCount: Long,
  editModeEnabled: Boolean,
  selected: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier.fillMaxWidth()) {
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
          photoUrl = null,
          fallback = groupName
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
          text = groupName,
          style = AppTheme.typography.title2Bold,
          color = AppTheme.colors.contentPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        GroupSubtitle(
          senderName = lastSenderName,
          lastCommit = lastCommit
        )
      }
      TrailingBlock(
        date = lastCommitAt,
        unreadCount = unreadCount
      )
    }
  }
}

@Composable
private fun GroupSubtitle(
  senderName: String?,
  lastCommit: String?
) {
  if (lastCommit == null) {
    Text(
      text = stringResource(R.string.conversation_group_no_messages),
      style = AppTheme.typography.body3,
      color = AppTheme.colors.contentSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    return
  }
  val text = if (senderName != null) "$senderName: $lastCommit" else lastCommit
  Text(
    text = text,
    style = AppTheme.typography.body3,
    color = AppTheme.colors.contentPrimary,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
  )
}

@Preview
@Composable
private fun GroupConversationItemPreviewLight() {
  PreviewColumn {
    GroupConversationItemPreviewContent()
  }
}

@Preview
@Composable
private fun GroupConversationItemPreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    GroupConversationItemPreviewContent()
  }
}

@Composable
private fun GroupConversationItemPreviewContent() {
  GroupConversationItem(
    groupName = "Команда дизайна",
    lastSenderName = "Алексей",
    lastCommit = "Закинул новые макеты в Figma",
    lastCommitAt = "12:34",
    unreadCount = 3,
    editModeEnabled = false,
    selected = false,
    onClick = {},
    onLongClick = {}
  )
  GroupConversationItem(
    groupName = "Книжный клуб",
    lastSenderName = null,
    lastCommit = null,
    lastCommitAt = null,
    unreadCount = 0,
    editModeEnabled = true,
    selected = true,
    onClick = {},
    onLongClick = {}
  )
}
