package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.core.ui.text.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.uikit.component.IconAction
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.scaffold.ScreenScaffold
import ru.sla.clarify.uikit.scaffold.rememberScreenScaffoldState
import ru.sla.clarify.uikit.theme.AppTheme

private val MergeProgressIndicatorSize = 24.dp

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
    ScreenScaffold(state = scaffoldState) {
      BranchReadyContent(
        state = state,
        intents = intents
      )
    }
  }
}

@Composable
internal fun BranchReadyContent(
  state: ViewState,
  intents: ViewIntents
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .imePadding()
  ) {
    TopBar(
      branchName = state.branchName,
      branchStatus = state.branchStatus,
      isMergeActionPending = state.isMergeActionPending,
      onBack = intents.navigateBack,
      onRequestMerge = intents.requestMerge
    )
    if (state.mergeRequest != null && state.branchStatus == Branch.Status.MergeInProgress) {
      MergeBanner(
        isMergeActionPending = state.isMergeActionPending,
        isCurrentUserApprover = state.isCurrentUserApprover,
        isCurrentUserInitiator = state.isCurrentUserInitiator,
        mergeRequest = state.mergeRequest,
        initiatorName = state.initiatorName,
        onApprove = intents.approveMerge,
        onRevoke = intents.revokeApproval
      )
    }
    if (state.commits.isEmpty()) {
      BranchEmptyState(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
    } else {
      Commits(
        commits = state.commits,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
    }
    HorizontalDivider()
    BottomArea(
      branchStatus = state.branchStatus,
      isSending = state.isSending,
      onSend = intents.sendCommit
    )
  }
}

@Composable
private fun TopBar(
  branchName: String?,
  branchStatus: Branch.Status,
  isMergeActionPending: Boolean,
  onBack: () -> Unit,
  onRequestMerge: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    IconAction(
      iconResId = R.drawable.ic_back_24,
      onClick = onBack
    )
    if (branchName != null) {
      Text(
        modifier = Modifier
          .weight(1f)
          .padding(start = 4.dp),
        text = branchName.ifEmpty { stringResource(R.string.branch_default_name) },
        style = AppTheme.typography.title1,
        fontWeight = FontWeight.SemiBold
      )
    }
    when (branchStatus) {
      Branch.Status.Active -> Button(
        onClick = onRequestMerge,
        enabled = !isMergeActionPending
      ) {
        Text(stringResource(R.string.branch_merge_button))
      }
      Branch.Status.MergeInProgress -> Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = stringResource(R.string.branch_merging),
          style = AppTheme.typography.body2,
          color = AppTheme.colors.textPrimary
        )
        CircularProgressIndicator(
          modifier = Modifier.size(MergeProgressIndicatorSize)
        )
      }
      Branch.Status.Merged -> Unit
    }
  }
  HorizontalDivider()
}

@Composable
private fun MergeBanner(
  isMergeActionPending: Boolean,
  isCurrentUserApprover: Boolean,
  isCurrentUserInitiator: Boolean,
  mergeRequest: Branch.MergeRequest,
  initiatorName: String?,
  onApprove: () -> Unit,
  onRevoke: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    when {
      isCurrentUserInitiator -> {
        Text(
          text = stringResource(R.string.branch_merge_waiting_for_peer),
          style = AppTheme.typography.body2,
          color = AppTheme.colors.textPrimary
        )
        TextButton(
          onClick = onRevoke,
          enabled = !isMergeActionPending
        ) {
          Text(stringResource(R.string.branch_merge_cancel_request))
        }
      }
      isCurrentUserApprover -> {
        Text(
          text = stringResource(R.string.branch_merge_approved_waiting),
          style = AppTheme.typography.body2,
          color = AppTheme.colors.textPrimary
        )
        TextButton(
          onClick = onRevoke,
          enabled = !isMergeActionPending
        ) {
          Text(stringResource(R.string.branch_merge_revoke_approval))
        }
      }
      else -> {
        Text(
          text = stringResource(
            R.string.branch_merge_request_summary,
            initiatorName?.takeIf { it.isNotBlank() } ?: mergeRequest.initiatorUid.value
          ),
          style = AppTheme.typography.body2,
          color = AppTheme.colors.textPrimary
        )
        Button(
          onClick = onApprove,
          enabled = !isMergeActionPending
        ) {
          Text(stringResource(R.string.branch_merge_approve_button))
        }
      }
    }
  }
  HorizontalDivider()
}

@Composable
private fun BottomArea(
  branchStatus: Branch.Status,
  isSending: Boolean,
  onSend: (String) -> Unit
) {
  when (branchStatus) {
    Branch.Status.Active -> InputRow(
      isSending = isSending,
      onSend = onSend,
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
    )
    Branch.Status.MergeInProgress -> LockedBanner(
      text = stringResource(R.string.branch_locked_merge_in_progress),
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
    )
    Branch.Status.Merged -> LockedBanner(
      text = stringResource(R.string.branch_merged_read_only),
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
    )
  }
}

@Composable
private fun Commits(
  commits: List<Commit>,
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()
  LaunchedEffect(commits.size) {
    if (commits.isNotEmpty()) {
      listState.animateScrollToItem(0)
    }
  }
  LazyColumn(
    modifier = modifier,
    state = listState,
    reverseLayout = true,
    verticalArrangement = Arrangement.spacedBy(
      space = 4.dp,
      alignment = Alignment.Bottom
    ),
    contentPadding = PaddingValues(8.dp)
  ) {
    items(
      items = commits,
      key = { it.id.value.ifEmpty { "${it.senderId.value}_${it.timestamp}_${it.text.hashCode()}" } }
    ) { commit ->
      CommitBubble(commit)
    }
  }
}

@Composable
private fun BranchEmptyState(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = stringResource(R.string.branch_empty_state),
      style = AppTheme.typography.body1
    )
  }
}

@Composable
private fun CommitBubble(commit: Commit) {
  val alignment = if (commit.isSelf) Alignment.End else Alignment.Start
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = alignment
  ) {
    Box(
      modifier = Modifier
        .surface(
          shape = AppTheme.shapes.round12,
          backgroundColor = Color(commit.colorHex.toColorInt())
        )
        .padding(
          vertical = 8.dp,
          horizontal = 12.dp
        )
    ) {
      Text(
        text = commit.text,
        style = AppTheme.typography.body1,
        color = AppTheme.colors.textPrimary
      )
    }
    val date = commit.timestamp.format(TIME_FORMATTER_HOUR_MINUTE)
    Text(
      text = when (commit.status) {
        Commit.Status.Sending -> stringResource(R.string.thread_commit_status_sending, date)
        Commit.Status.Failed -> stringResource(R.string.thread_commit_status_failed, date)
        Commit.Status.Sent -> date
      },
      color = AppTheme.colors.textPrimary,
      style = AppTheme.typography.caption2,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
  }
}

@Composable
private fun LockedBanner(
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
      color = AppTheme.colors.textPrimary
    )
  }
}

@Composable
private fun InputRow(
  isSending: Boolean,
  onSend: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    var inputValue by rememberSaveable { mutableStateOf("") }
    OutlinedTextField(
      modifier = Modifier.weight(1f),
      value = inputValue,
      onValueChange = { inputValue = it },
      placeholder = { Text(stringResource(R.string.chat_input_placeholder)) }
    )
    Button(
      onClick = {
        val text = inputValue.trim()
        if (text.isNotEmpty()) {
          onSend(text)
          inputValue = ""
        }
      },
      enabled = inputValue.isNotBlank() && !isSending
    ) {
      Text(stringResource(R.string.chat_input_send_button))
    }
  }
}
