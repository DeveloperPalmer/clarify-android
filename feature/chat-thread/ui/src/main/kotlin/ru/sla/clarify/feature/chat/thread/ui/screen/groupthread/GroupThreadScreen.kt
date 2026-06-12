package ru.sla.clarify.feature.chat.thread.ui.screen.groupthread

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chat.thread.ui.components.ChatCommits
import ru.sla.clarify.feature.chat.thread.ui.entity.Group
import ru.sla.clarify.uikit.component.avatar.GroupAvatar
import ru.sla.clarify.uikit.component.button.TertiaryIconButtonSmall
import ru.sla.clarify.uikit.component.textfield.ChatTextField
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.component.topappbar.rememberTopBarElevation
import ru.sla.clarify.uikit.keyboard.rememberKeyboardController
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
    BackHandler(onBack = intents.navigateBack)
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
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val topBarElevation = rememberTopBarElevation(listState)
    val keyboardController = rememberKeyboardController()
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
      },
      actions = {
        // TODO: @sla Group logic. Add branch logic as ThreadScreen
        TertiaryIconButtonSmall(
          modifier = Modifier.padding(end = 4.dp),
          iconRes = R.drawable.ic_git_branch_24,
          text = stringResource(R.string.thread_branches_count, 0),
          onClick = {
            scope.launch {
              keyboardController.awaitHide()
              // TODO: @sla Group logic. Add intents.showBranches() logic as ThreadScreen
            }
          }
        )
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
          commits = state.commits,
          onCommitsRead = {
            // TODO: @sla Group logic. Add onCommitsRead logic as ThreadScreen
          }
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
