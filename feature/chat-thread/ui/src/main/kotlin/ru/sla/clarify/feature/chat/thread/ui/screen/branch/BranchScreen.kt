package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
private val ApproverDotSize = 8.dp

// Цвета индикатора approve-статуса. Не привязаны к AppTheme — это семантические
// светофор-цвета (approved = зелёный, pending = жёлтый), они одинаковы в light/dark
// темах. Если в проекте появятся semantic accent colors — заменить здесь.
private val ApproverDotApprovedColor = Color(0xFF4CAF50)
private val ApproverDotPendingColor = Color(0xFFFFC107)

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
      mergeRequestStatus = state.mergeRequest?.status,
      isMergeActionPending = state.mergeRequestRunning,
      onBack = intents.navigateBack,
      onRequestMerge = intents.openMergeRequest
    )
    if (state.mergeRequest != null &&
      state.mergeRequest.status != Branch.MergeRequest.Status.Merged
    ) {
      MergeBanner(
        state = state,
        onApprove = intents.approveMergeRequest,
        onRevoke = intents.revokeApprovalMergeRequest,
        onCancel = intents.cancelMergeRequest,
        onFinalize = intents.finalizeMergeRequest
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
      state = state,
      onSend = intents.sendCommit
    )
  }
}

@Composable
private fun TopBar(
  branchName: String?,
  mergeRequestStatus: Branch.MergeRequest.Status?,
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
    when (mergeRequestStatus) {
      null -> Button(
        onClick = onRequestMerge,
        enabled = !isMergeActionPending
      ) {
        Text(stringResource(R.string.branch_open_mr_button))
      }
      Branch.MergeRequest.Status.Open -> MergeStatusLabel(
        text = stringResource(R.string.branch_merge_in_progress)
      )
      Branch.MergeRequest.Status.ReadyToMerge -> MergeStatusLabel(
        text = stringResource(R.string.branch_merge_ready),
        showProgress = false
      )
      Branch.MergeRequest.Status.Merged -> Unit
    }
  }
  HorizontalDivider()
}

@Composable
private fun MergeStatusLabel(
  text: String,
  showProgress: Boolean = true
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = text,
      style = AppTheme.typography.body2,
      color = AppTheme.colors.onSurface
    )
    if (showProgress) {
      CircularProgressIndicator(
        modifier = Modifier.size(MergeProgressIndicatorSize)
      )
    }
  }
}

@Composable
private fun MergeBanner(
  state: ViewState,
  onApprove: () -> Unit,
  onRevoke: () -> Unit,
  onCancel: () -> Unit,
  onFinalize: () -> Unit
) {
  val mergeRequest = state.mergeRequest ?: return
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = stringResource(
        R.string.branch_merge_request_summary,
        state.initiatorName?.takeIf { it.isNotBlank() } ?: mergeRequest.initiatorUid.value
      ),
      style = AppTheme.typography.body2,
      color = AppTheme.colors.onSurface
    )
    if (state.isCurrentUserApproved) {
      Text(
        text = stringResource(R.string.branch_merge_approved_waiting),
        style = AppTheme.typography.body2,
        color = AppTheme.colors.onSurface
      )
    }
    if (state.approvers.isNotEmpty()) {
      ApproversRow(approvers = state.approvers)
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Approve / Revoke: видна на статусах Open и ReadyToMerge.
      if (!state.isCurrentUserApproved) {
        Button(
          onClick = onApprove,
          enabled = !state.mergeRequestRunning
        ) {
          Text(stringResource(R.string.branch_merge_approve_button))
        }
      } else {
        TextButton(
          onClick = onRevoke,
          enabled = !state.mergeRequestRunning
        ) {
          Text(stringResource(R.string.branch_merge_revoke_approval))
        }
      }
      // Merge now: только на ReadyToMerge, доступна любому участнику.
      if (mergeRequest.status == Branch.MergeRequest.Status.ReadyToMerge) {
        Button(
          onClick = onFinalize,
          enabled = !state.mergeRequestRunning
        ) {
          Text(stringResource(R.string.branch_merge_finalize_button))
        }
      }
      // Cancel: доступна любому участнику, не зависит от инициатора.
      TextButton(
        onClick = onCancel,
        enabled = !state.mergeRequestRunning
      ) {
        Text(stringResource(R.string.branch_merge_cancel_request))
      }
    }
  }
  HorizontalDivider()
}

@Composable
private fun BottomArea(
  state: ViewState,
  onSend: (String) -> Unit
) {
  val modifier = Modifier
    .fillMaxWidth()
    .navigationBarsPadding()
  when (state.mergeRequest?.status) {
    null -> InputRow(
      isSending = state.isSending,
      onSend = onSend,
      modifier = modifier
    )
    Branch.MergeRequest.Status.Open,
    Branch.MergeRequest.Status.ReadyToMerge -> LockedBanner(
      text = stringResource(R.string.branch_locked_merge_in_progress),
      modifier = modifier
    )
    Branch.MergeRequest.Status.Merged -> LockedBanner(
      text = stringResource(R.string.branch_merged_read_only),
      modifier = modifier
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
        color = AppTheme.colors.onSurface
      )
    }
    val date = commit.timestamp.format(TIME_FORMATTER_HOUR_MINUTE)
    Text(
      text = when (commit.status) {
        Commit.Status.Sending -> stringResource(R.string.thread_commit_status_sending, date)
        Commit.Status.Failed -> stringResource(R.string.thread_commit_status_failed, date)
        Commit.Status.Sent -> date
      },
      color = AppTheme.colors.onSurface,
      style = AppTheme.typography.caption,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
  }
}

@Composable
private fun ApproversRow(approvers: List<Approver>) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    approvers.forEach { approver ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(ApproverDotSize)
            .background(
              color = if (approver.isApproved) {
                ApproverDotApprovedColor
              } else {
                ApproverDotPendingColor
              },
              shape = CircleShape
            )
        )
        Text(
          text = approver.displayName?.takeIf { it.isNotBlank() } ?: approver.userId.value,
          style = AppTheme.typography.body2,
          color = AppTheme.colors.onSurface
        )
      }
    }
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
      color = AppTheme.colors.onSurface
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
