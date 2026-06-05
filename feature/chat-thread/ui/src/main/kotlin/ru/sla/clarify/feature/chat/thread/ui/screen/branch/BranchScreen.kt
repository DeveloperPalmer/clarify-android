package ru.sla.clarify.feature.chat.thread.ui.screen.branch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.screen.MviComponent
import ru.sla.clarify.core.ui.screen.rememberViewIntents
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.components.ChatEmptyState
import ru.sla.clarify.feature.chat.thread.ui.components.ChatInput
import ru.sla.clarify.feature.chat.thread.ui.components.Commits
import ru.sla.clarify.feature.chat.thread.ui.components.ScrollToBottomFab
import ru.sla.clarify.feature.chat.thread.ui.components.rememberTopBarElevation
import ru.sla.clarify.uikit.component.topappbar.TopAppBar
import ru.sla.clarify.uikit.component.topappbar.TopAppBarDefaults
import ru.sla.clarify.uikit.modifier.bottomShadow
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
  val listState = rememberLazyListState()
  val topBarElevation = rememberTopBarElevation(listState)
  val topBarModifier = remember(topBarElevation) {
    Modifier.bottomShadow { topBarElevation.value }
  }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .systemBarsPadding()
      .imePadding()
  ) {
    BranchTopAppBar(
      modifier = topBarModifier,
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
      ChatEmptyState(
        text = stringResource(R.string.branch_empty_state),
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      )
    } else {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        Commits(
          modifier = Modifier.fillMaxSize(),
          listState = listState,
          commits = state.commits,
          onCommitsRead = intents.markReadUpTo
        )
        ScrollToBottomFab(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
          listState = listState,
          unreadCount = state.unreadCount
        )
      }
    }
    BottomArea(
      state = state,
      onSend = intents.sendCommit
    )
  }
}

@Composable
private fun BranchTopAppBar(
  branchName: String?,
  mergeRequestStatus: Branch.MergeRequest.Status?,
  isMergeActionPending: Boolean,
  onBack: () -> Unit,
  onRequestMerge: () -> Unit,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    modifier = modifier,
    navigationIcon = { TopAppBarDefaults.NavigationIcon(onBack) },
    title = { branchName?.let { TopAppBarContent(branchName = it) } },
    actions = {
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
  )
}

@Composable
private fun TopAppBarContent(
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
      color = AppTheme.colors.contentPrimary
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
        state.initiatorName?.takeIf { it.isNotBlank() } ?: mergeRequest.initiatorId.value
      ),
      style = AppTheme.typography.body2,
      color = AppTheme.colors.contentPrimary
    )
    if (state.isCurrentUserApproved) {
      Text(
        text = stringResource(R.string.branch_merge_approved_waiting),
        style = AppTheme.typography.body2,
        color = AppTheme.colors.contentPrimary
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
  val modifier = Modifier.fillMaxWidth()
  when (state.mergeRequest?.status) {
    null -> ChatInput(
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
          color = AppTheme.colors.contentPrimary
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
      color = AppTheme.colors.contentPrimary
    )
  }
}
