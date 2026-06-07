package ru.sla.clarify.feature.chat.thread.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch.MergeRequest.Status
import ru.sla.clarify.feature.chat.thread.ui.entity.Approver
import ru.sla.clarify.feature.chat.thread.ui.screen.branch.MERGE_REQUEST_MOTION_KEY
import ru.sla.clarify.uikit.animation.LocalSharedTransitionScope
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.VSpacer

@Composable
internal fun MergeRequestCard(
  visible: Boolean,
  mergeRequest: Branch.MergeRequest,
  initiatorName: String?,
  isCurrentUserApproved: Boolean,
  mergeRequestRunning: Boolean,
  approvers: List<Approver>,
  onApprove: () -> Unit,
  onRevoke: () -> Unit,
  onCancel: () -> Unit,
  onFinalize: () -> Unit,
  modifier: Modifier = Modifier
) = with(LocalSharedTransitionScope.current) {
  AnimatedVisibility(
    modifier = modifier,
    visible = visible,
    enter = fadeIn(tween(AppTheme.motion.shortMillis)),
    exit = fadeOut(tween(AppTheme.motion.shortMillis))
  ) {
    Column(
      modifier = Modifier
        .sharedBounds(
          sharedContentState = rememberSharedContentState(MERGE_REQUEST_MOTION_KEY),
          animatedVisibilityScope = this,
          boundsTransform = AppTheme.motion.mediumBoundsTransform(),
          resizeMode = RemeasureToBounds
        )
        .surface(
          shape = AppTheme.shapes.round16,
          elevation = AppTheme.elevation.largest,
          backgroundColor = AppTheme.colors.cardPrimary
        )
    ) {
      VSpacer(8.dp)
      IslandHandle()
      MergeRequestBanner(
        modifier = Modifier
          .animateContentSize(AppTheme.motion.mediumTween())
          .fillMaxWidth()
          .padding(16.dp, 12.dp),
        mergeRequest = mergeRequest,
        initiatorName = initiatorName,
        isCurrentUserApproved = isCurrentUserApproved,
        mergeRequestRunning = mergeRequestRunning,
        approvers = approvers,
        onApprove = onApprove,
        onRevoke = onRevoke,
        onCancel = onCancel,
        onFinalize = onFinalize
      )
      VSpacer(8.dp)
    }
  }
}

@Composable
private fun MergeRequestBanner(
  mergeRequest: Branch.MergeRequest,
  initiatorName: String?,
  isCurrentUserApproved: Boolean,
  mergeRequestRunning: Boolean,
  approvers: List<Approver>,
  onApprove: () -> Unit,
  onRevoke: () -> Unit,
  onCancel: () -> Unit,
  onFinalize: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = stringResource(
        R.string.branch_merge_request_summary,
        initiatorName?.takeIf { it.isNotBlank() } ?: mergeRequest.initiatorId.value
      ),
      style = AppTheme.typography.body2,
      color = AppTheme.colors.contentPrimary
    )
    if (isCurrentUserApproved) {
      Text(
        text = stringResource(R.string.branch_merge_approved_waiting),
        style = AppTheme.typography.body2,
        color = AppTheme.colors.contentPrimary
      )
    }
    if (approvers.isNotEmpty()) {
      ApproversRow(approvers = approvers)
    }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Approve / Revoke: видна на статусах Open и ReadyToMerge.
      if (!isCurrentUserApproved) {
        Button(
          onClick = onApprove,
          enabled = !mergeRequestRunning
        ) {
          Text(stringResource(R.string.branch_merge_approve_button))
        }
      } else {
        TextButton(
          onClick = onRevoke,
          enabled = !mergeRequestRunning
        ) {
          Text(stringResource(R.string.branch_merge_revoke_approval))
        }
      }
      // Merge now: только на ReadyToMerge, доступна любому участнику.
      if (mergeRequest.status == Status.ReadyToMerge) {
        Button(
          onClick = onFinalize,
          enabled = !mergeRequestRunning
        ) {
          Text(stringResource(R.string.branch_merge_finalize_button))
        }
      }
      // Cancel: доступна любому участнику, не зависит от инициатора.
      TextButton(
        onClick = onCancel,
        enabled = !mergeRequestRunning
      ) {
        Text(stringResource(R.string.branch_merge_cancel_request))
      }
    }
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

@Composable
private fun IslandHandle() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .size(width = 32.dp, height = 4.dp)
        .background(color = AppTheme.colors.contentTertiary, shape = CircleShape)
    )
  }
}
