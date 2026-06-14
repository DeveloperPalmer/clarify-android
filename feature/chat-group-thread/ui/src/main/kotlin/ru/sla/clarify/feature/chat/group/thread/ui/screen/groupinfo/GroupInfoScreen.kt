package ru.sla.clarify.feature.chat.group.thread.ui.screen.groupinfo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chat.group.thread.ui.entity.Group
import ru.sla.clarify.feature.chat.group.thread.ui.entity.GroupMember
import ru.sla.clarify.uikit.component.Divider
import ru.sla.clarify.uikit.component.avatar.Avatar
import ru.sla.clarify.uikit.component.avatar.GroupAvatar
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun GroupInfoScreen(viewModel: GroupInfoViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    BackHandler(onBack = intents.navigateBack)
    val group = state.group
    if (group != null) {
      GroupInfoReadyContent(
        group = group,
        members = state.members,
        isOwner = state.isOwner,
        intents = intents
      )
    }
  }
}

@Composable
private fun GroupInfoReadyContent(
  group: Group,
  members: List<GroupMember>,
  isOwner: Boolean,
  intents: ViewIntents
) {
  GroupInfoContent(
    modifier = Modifier
      .fillMaxSize()
      .systemBarsPadding(),
    group = group,
    members = members,
    isOwner = isOwner,
    onBack = intents.navigateBack,
    onRename = intents.showRenameDialog,
    onAddMembers = intents.showInviteSheet,
    onRemoveMember = intents.requestRemoveMember,
    onLeaveGroup = intents.requestLeaveGroup,
    onDeleteGroup = intents.requestDeleteGroup
  )
}

@Composable
fun GroupInfoContent(
  group: Group,
  members: List<GroupMember>,
  isOwner: Boolean,
  onBack: () -> Unit,
  onRename: () -> Unit,
  onAddMembers: () -> Unit,
  onRemoveMember: (GroupMember) -> Unit,
  onLeaveGroup: () -> Unit,
  onDeleteGroup: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    TopAppBar(
      navigationIcon = { TopAppBarDefaults.NavigationIcon(onBack) },
      title = {
        Text(
          text = stringResource(R.string.group_info_title),
          style = AppTheme.typography.title2Bold,
          color = AppTheme.colors.contentPrimary
        )
      }
    )
    LazyColumn(modifier = Modifier.weight(1f)) {
      item {
        GroupHeader(
          group = group,
          isOwner = isOwner,
          onRename = onRename
        )
      }
      if (isOwner) {
        item {
          ActionRow(
            iconRes = R.drawable.ic_person_plus_24,
            text = stringResource(R.string.group_info_add_members),
            onClick = onAddMembers
          )
        }
      }
      item {
        SectionHeader(
          text = stringResource(
            R.string.group_info_section_members,
            members.size
          ).uppercase()
        )
      }
      items(
        count = members.size,
        key = { members[it].id }
      ) { index ->
        val member = members[index]
        MemberRow(
          member = member,
          canRemove = isOwner && !member.isOwner && !member.isMe,
          onRemove = { onRemoveMember(member) }
        )
        if (index < members.lastIndex) {
          Divider(
            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
            color = AppTheme.colors.cardQuinary
          )
        }
      }
      item {
        DestructiveFooter(
          isOwner = isOwner,
          onLeave = onLeaveGroup,
          onDelete = onDeleteGroup
        )
      }
    }
  }
}

@Composable
private fun GroupHeader(
  group: Group,
  isOwner: Boolean,
  onRename: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    GroupAvatar(
      size = 96.dp,
      name = group.name,
      colorSeed = group.id
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(
        text = group.name,
        style = AppTheme.typography.headline3Bold,
        color = AppTheme.colors.contentPrimary,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      if (isOwner) {
        Icon(
          modifier = Modifier
            .size(20.dp)
            .clickable(onClick = onRename),
          painter = painterResource(R.drawable.ic_pencil_24),
          tint = AppTheme.colors.contentSecondary,
          contentDescription = stringResource(R.string.group_info_rename)
        )
      }
    }
    Text(
      text = pluralStringResource(
        R.plurals.group_info_members_count,
        group.memberCount,
        group.memberCount
      ),
      style = AppTheme.typography.body2,
      color = AppTheme.colors.contentSecondary
    )
  }
}

@Composable
private fun ActionRow(
  iconRes: Int,
  text: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(AppTheme.colors.backgroundAccentPrimary),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        painter = painterResource(iconRes),
        tint = AppTheme.colors.contentAccentPrimary,
        contentDescription = null
      )
    }
    Text(
      text = text,
      style = AppTheme.typography.body1,
      color = AppTheme.colors.contentPrimary
    )
  }
}

@Composable
private fun SectionHeader(text: String) {
  Text(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    text = text,
    style = AppTheme.typography.label2Bold,
    color = AppTheme.colors.contentSecondary
  )
}

@Composable
private fun MemberRow(
  member: GroupMember,
  canRemove: Boolean,
  onRemove: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Avatar(
      size = 40.dp,
      photoUrl = member.photoUrl,
      fallbackInitial = member.displayName
    )
    Column(modifier = Modifier.weight(1f)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = member.displayName,
          style = AppTheme.typography.body1,
          color = AppTheme.colors.contentPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (member.isMe) {
          Text(
            text = "· ${stringResource(R.string.group_info_you_badge)}",
            style = AppTheme.typography.label3,
            color = AppTheme.colors.contentSecondary
          )
        }
      }
      Text(
        text = member.email,
        style = AppTheme.typography.label3,
        color = AppTheme.colors.contentSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    if (member.isOwner) {
      OwnerBadge()
    } else if (canRemove) {
      Icon(
        modifier = Modifier
          .size(20.dp)
          .clickable(onClick = onRemove),
        painter = painterResource(R.drawable.ic_trash_24),
        tint = AppTheme.colors.errorPrimary,
        contentDescription = stringResource(R.string.group_info_remove_member)
      )
    }
  }
}

@Composable
private fun OwnerBadge() {
  Box(
    modifier = Modifier
      .clip(AppTheme.shapes.round12)
      .background(AppTheme.colors.backgroundAccentPrimary)
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      text = stringResource(R.string.group_info_owner_badge),
      style = AppTheme.typography.label3Bold,
      color = AppTheme.colors.contentAccentPrimary
    )
  }
}

@Composable
private fun DestructiveFooter(
  isOwner: Boolean,
  onLeave: () -> Unit,
  onDelete: () -> Unit
) {
  val (iconRes, label, action) = if (isOwner) {
    Triple(R.drawable.ic_trash_24, R.string.group_info_delete, onDelete)
  } else {
    Triple(R.drawable.ic_logout_24, R.string.group_info_leave, onLeave)
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = action)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(AppTheme.colors.errorSecondary),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        painter = painterResource(iconRes),
        tint = AppTheme.colors.errorPrimary,
        contentDescription = null
      )
    }
    Text(
      text = stringResource(label),
      style = AppTheme.typography.body1,
      color = AppTheme.colors.errorPrimary
    )
  }
}
