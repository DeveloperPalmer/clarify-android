package ru.sla.clarify.feature.chat.thread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch.MergeRequest.Status
import ru.sla.clarify.feature.chat.thread.ui.entity.Approver
import ru.sla.clarify.feature.chat.thread.ui.screen.branch.ViewState
import ru.sla.clarify.uikit.theme.AppTheme

@Composable
internal fun MergeStatusLabel(
  text: String,
  modifier: Modifier = Modifier,
  showProgress: Boolean = true
) {
  Row(
    modifier = modifier,
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
        modifier = Modifier.size(24.dp)
      )
    }
  }
}

@Composable
internal fun MergeBanner(
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
      if (mergeRequest.status == Status.ReadyToMerge) {
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
            .size(8.dp)
            .background(
              color = if (approver.isApproved) {
                AppTheme.colors.successPrimary
              } else {
                AppTheme.colors.contentGoldPrimary
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
