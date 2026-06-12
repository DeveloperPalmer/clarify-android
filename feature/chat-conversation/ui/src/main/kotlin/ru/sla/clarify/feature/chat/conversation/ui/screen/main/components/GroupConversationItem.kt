package ru.sla.clarify.feature.chat.conversation.ui.screen.main.components

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
import ru.sla.clarify.uikit.component.UnreadCountBadge
import ru.sla.clarify.uikit.component.avatar.GroupAvatar
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

@Composable
internal fun GroupConversationItem(
  groupId: String,
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
        GroupAvatar(
          size = 40.dp,
          name = groupName,
          colorSeed = groupId
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
      GroupTrailingBlock(
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

@Composable
private fun GroupTrailingBlock(
  date: String?,
  unreadCount: Long,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
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
    groupId = "g-1",
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
    groupId = "g-2",
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
