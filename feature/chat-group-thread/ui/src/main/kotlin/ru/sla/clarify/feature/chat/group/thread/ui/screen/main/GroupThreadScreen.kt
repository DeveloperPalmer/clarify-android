package ru.sla.clarify.feature.chat.group.thread.ui.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chat.group.thread.ui.entity.Group
import ru.sla.clarify.uikit.component.avatar.Avatar
import ru.sla.clarify.uikit.component.chat.ChatCommits
import ru.sla.clarify.uikit.component.textfield.ChatTextField
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.component.topappbar.rememberTopBarElevation
import ru.sla.clarify.uikit.modifier.bottomShadow
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun GroupThreadScreen(viewModel: GroupThreadViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    val scaffoldState = rememberScreenScaffoldState()
    scaffoldState.contentLoadState = state.contentLoadState
    ScreenScaffold(scaffoldState) {
      GroupThreadContent(
        modifier = Modifier
          .fillMaxSize()
          .systemBarsPadding()
          .imePadding(),
        state = state,
        intents = intents
      )
    }
  }
}

@Composable
private fun GroupThreadContent(
  state: ViewState,
  intents: ViewIntents,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    val listState = rememberLazyListState()
    val topBarElevation = rememberTopBarElevation(listState)
    TopAppBar(
      modifier = Modifier.bottomShadow { topBarElevation.value },
      navigationIcon = { TopAppBarDefaults.NavigationIcon(intents.navigateBack) },
      title = {
        if (state.group != null) {
          GroupHeader(
            group = state.group,
            onClick = intents.openGroupInfo
          )
        }
      }
    )
    Box(modifier = Modifier.weight(1f)) {
      if (state.group != null && state.commits.isEmpty()) {
        EmptyState(
          group = state.group
        )
      } else if (state.commits.isNotEmpty()) {
        ChatCommits(
          modifier = Modifier.fillMaxSize(),
          listState = listState,
          items = state.commits,
          onRead = intents.markReadUpTo,
          hasHistory = false,
          loadingHistory = false,
          selectionEnabled = false,
          focused = null,
          onLoad = {},
          onClick = {},
          onLongClick = {}
        )
      }
    }
    var inputValue by rememberSaveable { mutableStateOf("") }
    ChatTextField(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 8.dp),
      value = inputValue,
      onValueChange = {
        inputValue = it
      },
      onClear = {
        inputValue = ""
      },
      onSend = {
        intents.sendMessage(inputValue)
        inputValue = ""
      }
    )
  }
}

@Composable
private fun GroupHeader(
  group: Group,
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
    Avatar(
      size = 40.dp,
      photoUrl = null,
      fallback = group.name
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
private fun EmptyState(group: Group) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Avatar(
        size = 88.dp,
        photoUrl = null,
        fallback = group.name
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
