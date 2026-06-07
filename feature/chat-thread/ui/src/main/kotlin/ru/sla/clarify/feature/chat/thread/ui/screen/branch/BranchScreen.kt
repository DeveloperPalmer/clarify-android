package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch.MergeRequest.Status
import ru.sla.clarify.feature.chat.thread.ui.components.ChatCommits
import ru.sla.clarify.feature.chat.thread.ui.components.ChatEmptyState
import ru.sla.clarify.feature.chat.thread.ui.components.MergeRequestButton
import ru.sla.clarify.feature.chat.thread.ui.components.MergeRequestCard
import ru.sla.clarify.uikit.component.button.ChatScrollToBottomButton
import ru.sla.clarify.uikit.component.scrim.ScrimEffect
import ru.sla.clarify.uikit.component.textfield.ChatTextField
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.component.topappbar.rememberTopBarElevation
import ru.sla.clarify.uikit.modifier.bottomShadow
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
fun BranchScreen(viewModel: BranchViewModel) {
  MviComponent(
    viewModel = viewModel,
    intents = rememberViewIntents()
  ) { state, intents ->
    val scaffoldState = rememberScreenScaffoldState()
    scaffoldState.contentLoadState = state.contentLoadState
    BackHandler(
      onBack = intents.navigateBack
    )
    ScreenScaffold(scaffoldState) {
      BranchReadyContent(
        modifier = Modifier
          .fillMaxSize()
          .systemBarsPadding()
          .imePadding(),
        state = state,
        intents = intents
      )
      if (state.mergeRequest != null) {
        ScrimEffect(
          visible = state.mergeRequestVisible,
          onFinish = intents.hideMergeRequest
        )
        MergeRequestCard(
          modifier = Modifier
            .systemBarsPadding()
            .padding(start = 12.dp, end = 12.dp),
          visible = state.mergeRequestVisible,
          mergeRequest = state.mergeRequest,
          initiatorName = state.initiatorName,
          approvers = state.approvers,
          isCurrentUserApproved = state.isCurrentUserApproved,
          mergeRequestInProgress = state.mergeRequestInProgress,
          onApprove = intents.approveMergeRequest,
          onRevoke = intents.revokeApprovalMergeRequest,
          onCancel = intents.cancelMergeRequest,
          onFinalize = intents.finalizeMergeRequest
        )
      }
    }
  }
}

@Composable
internal fun BranchReadyContent(
  state: ViewState,
  intents: ViewIntents,
  modifier: Modifier = Modifier
) {
  BackHandler(enabled = state.mergeRequestVisible) {
    intents.hideMergeRequest()
  }
  Column(modifier = modifier) {
    val listState = rememberLazyListState()
    val topBarElevation = rememberTopBarElevation(listState)
    TopAppBar(
      modifier = Modifier.bottomShadow { topBarElevation.value },
      navigationIcon = { TopAppBarDefaults.NavigationIcon(intents.navigateBack) },
      title = { state.branchName?.let { TopAppBarCenterContent(branchName = it) } },
      actions = {
        MergeRequestButton(
          visible = !state.mergeRequestVisible,
          inProgress = state.mergeRequestInProgress,
          status = state.mergeRequest?.status,
          onOpenMergeRequest = intents.openMergeRequest
        )
      }
    )
    Box(modifier = Modifier.weight(1f)) {
      ChatCommits(
        modifier = Modifier.fillMaxSize(),
        listState = listState,
        commits = state.commits,
        onCommitsRead = intents.markReadUpTo
      )
      ChatScrollToBottomButton(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 16.dp, bottom = 16.dp),
        listState = listState,
        unreadCount = state.unreadCount
      )
      ChatEmptyState(
        modifier = Modifier.fillMaxSize(),
        visible = state.commits.isEmpty(),
        text = stringResource(R.string.branch_empty_state)
      )
    }
    BottomArea(
      mergeRequestStatus = state.mergeRequest?.status,
      onSend = intents.sendCommit
    )
  }
}

@Composable
private fun TopAppBarCenterContent(
  branchName: String,
  modifier: Modifier = Modifier
) {
  Text(
    modifier = modifier,
    text = branchName.ifEmpty { stringResource(R.string.branch_default_name) },
    style = AppTheme.typography.title2Bold
  )
}

@Composable
private fun BottomArea(
  mergeRequestStatus: Status?,
  onSend: (String) -> Unit
) {
  when (mergeRequestStatus) {
    null -> {
      var inputValue by rememberSaveable { mutableStateOf("") }
      ChatTextField(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        value = inputValue,
        onValueChange = { inputValue = it },
        onSend = { onSend(inputValue) },
        onClear = { inputValue = "" }
      )
    }
    Status.Open,
    Status.ReadyToMerge -> {
      LockedMessage(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.branch_locked_merge_in_progress)
      )
    }
    Status.Merged -> {
      LockedMessage(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.branch_merged_read_only)
      )
    }
  }
}

@Composable
private fun LockedMessage(
  text: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      style = AppTheme.typography.body2,
      color = AppTheme.colors.contentPrimary
    )
  }
}
