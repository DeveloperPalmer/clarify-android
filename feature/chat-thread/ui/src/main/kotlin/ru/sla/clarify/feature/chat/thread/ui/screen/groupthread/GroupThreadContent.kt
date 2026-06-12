package ru.sla.clarify.feature.chat.thread.ui.screen.groupthread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chat.thread.ui.entity.GroupUi
import ru.sla.clarify.uikit.component.GroupAvatar
import ru.sla.clarify.uikit.component.SystemMessageItem
import ru.sla.clarify.uikit.component.bubble.BubbleMessage
import ru.sla.clarify.uikit.component.textfield.ChatTextField
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

@Composable
fun GroupThreadContent(
  group: GroupUi,
  items: List<GroupThreadItem>,
  inputValue: String,
  onInputChange: (String) -> Unit,
  onBack: () -> Unit,
  onSend: () -> Unit,
  onOpenGroupInfo: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    TopAppBar(
      navigationIcon = { TopAppBarDefaults.NavigationIcon(onBack) },
      title = {
        GroupHeader(
          group = group,
          onClick = onOpenGroupInfo
        )
      }
    )
    Box(modifier = Modifier.weight(1f)) {
      if (items.isEmpty()) {
        EmptyState(group = group)
      } else {
        ThreadList(items = items)
      }
    }
    ChatTextField(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 8.dp),
      value = inputValue,
      onValueChange = onInputChange,
      onSend = onSend,
      onClear = { onInputChange("") }
    )
  }
}

@Composable
private fun GroupHeader(
  group: GroupUi,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .surface(
        backgroundColor = AppTheme.colors.backgroundPrimary,
        shape = AppTheme.shapes.round12,
        onClick = onClick
      )
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    GroupAvatar(
      size = 40.dp,
      name = group.name,
      colorSeed = group.id
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = group.name,
        style = AppTheme.typography.title2Bold,
        color = AppTheme.colors.contentPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = pluralStringResource(
          R.plurals.group_info_members_count,
          group.memberCount,
          group.memberCount
        ),
        style = AppTheme.typography.body3,
        color = AppTheme.colors.contentSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun ThreadList(items: List<GroupThreadItem>) {
  val listState = rememberLazyListState()
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    state = listState,
    reverseLayout = true,
    contentPadding = PaddingValues(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)
  ) {
    items(
      count = items.size,
      key = { items[it].key }
    ) { index ->
      when (val item = items[index]) {
        is GroupThreadItem.Bubble -> BubbleMessage(bubble = item.bubble)
        is GroupThreadItem.System -> SystemMessageItem(
          modifier = Modifier.fillMaxWidth(),
          text = item.text
        )
      }
    }
  }
}

@Composable
private fun EmptyState(group: GroupUi) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      GroupAvatar(
        size = 88.dp,
        name = group.name,
        colorSeed = group.id
      )
      Text(
        text = group.name,
        style = AppTheme.typography.headline3Bold,
        color = AppTheme.colors.contentPrimary,
        textAlign = TextAlign.Center
      )
      Text(
        text = pluralStringResource(
          R.plurals.group_info_members_count,
          group.memberCount,
          group.memberCount
        ),
        style = AppTheme.typography.body3,
        color = AppTheme.colors.contentSecondary
      )
      Text(
        text = stringResource(R.string.group_thread_empty),
        style = AppTheme.typography.body2,
        color = AppTheme.colors.contentSecondary,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Preview
@Composable
private fun GroupThreadContentPreviewLight() {
  PreviewColumn {
    GroupThreadContentPreviewContent()
  }
}

@Preview
@Composable
private fun GroupThreadContentPreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    GroupThreadContentPreviewContent()
  }
}

@Composable
private fun GroupThreadContentPreviewContent() {
  var input by rememberSaveable { mutableStateOf("") }
  val anna = BubbleMessage.Sender(id = "anna", name = "Аня Котова")
  val ilya = BubbleMessage.Sender(id = "ilya", name = "Илья Соколов")
  val items = listOf(
    GroupThreadItem.Bubble(
      BubbleMessage(
        id = BubbleMessage.Id("m1"),
        type = BubbleMessage.Type.Top,
        side = BubbleMessage.Side.Left,
        text = "Привет команде!",
        time = "12:30",
        sender = anna
      )
    ),
    GroupThreadItem.System(id = "s1", text = "Аня пригласила Илью"),
    GroupThreadItem.Bubble(
      BubbleMessage(
        id = BubbleMessage.Id("m2"),
        type = BubbleMessage.Type.Top,
        side = BubbleMessage.Side.Left,
        text = "Здарова",
        time = "12:31",
        sender = ilya
      )
    ),
    GroupThreadItem.Bubble(
      BubbleMessage(
        id = BubbleMessage.Id("m3"),
        type = BubbleMessage.Type.Bottom,
        side = BubbleMessage.Side.Right(status = BubbleMessage.ReadStatus.Read),
        text = "Стартуем",
        time = "12:32"
      )
    )
  ).reversed()
  GroupThreadContent(
    group = GroupUi(id = "g-1", name = "Команда дизайна", memberCount = 5),
    items = items,
    inputValue = input,
    onInputChange = { input = it },
    onBack = {},
    onSend = {},
    onOpenGroupInfo = {}
  )
}
