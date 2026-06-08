package ru.sla.clarify.feature.chat.thread.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.domain.randomUuid
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.entity.Approver
import ru.sla.clarify.feature.chat.thread.ui.screen.branch.MERGE_REQUEST_MOTION_KEY
import ru.sla.clarify.feature.entity.chat.Participant
import ru.sla.clarify.uikit.animation.LocalSharedTransitionScope
import ru.sla.clarify.uikit.animation.SharedContainer
import ru.sla.clarify.uikit.component.Avatar
import ru.sla.clarify.uikit.component.RevealSplitRow
import ru.sla.clarify.uikit.component.button.ButtonStyle
import ru.sla.clarify.uikit.component.button.PrimaryButton
import ru.sla.clarify.uikit.component.button.SecondaryTextButton
import ru.sla.clarify.uikit.component.icon.IconAction
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.clarify.uikit.theme.HSpacer
import ru.sla.clarify.uikit.theme.VSpacer
import ru.sla.clarify.uikit.theme.WSpacer

@Composable
internal fun MergeRequestCard(
  visible: Boolean,
  mergeRequest: Branch.MergeRequest?,
  isCurrentUserApproved: Boolean,
  mergeRequestInProgress: Boolean,
  participants: List<Participant>,
  approvers: List<Approver>,
  onApprove: () -> Unit,
  onRevoke: () -> Unit,
  onCancel: () -> Unit,
  onFinalize: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  SharedContainer(
    modifier = modifier,
    key = MERGE_REQUEST_MOTION_KEY,
    visible = visible,
    restingCorner = MERGE_REQUEST_CARD_CORNER,
    morphedCorner = MERGE_REQUEST_BUTTON_CORNER
  ) {
    Box(
      modifier = Modifier.sharedSurface(
        elevation = AppTheme.elevation.largest,
        color = AppTheme.colors.cardPrimary
      )
    ) {
      Column(
        modifier = Modifier
          .revealContent(MERGE_REQUEST_CARD_REVEAL_WINDOW)
          .then(with(sharedTransitionScope) { Modifier.skipToLookaheadSize() })
          .animateContentSize(AppTheme.motion.mediumTween())
          .fillMaxWidth()
      ) {
        MergeRequestBanner(
          mergeRequest = mergeRequest,
          isCurrentUserApproved = isCurrentUserApproved,
          mergeRequestInProgress = mergeRequestInProgress,
          participants = participants,
          approvers = approvers,
          onApprove = onApprove,
          onRevoke = onRevoke,
          onCancel = onCancel,
          onFinalize = onFinalize
        )
      }
    }
  }
}

@Composable
private fun MergeRequestBanner(
  mergeRequest: Branch.MergeRequest?,
  isCurrentUserApproved: Boolean,
  mergeRequestInProgress: Boolean,
  participants: List<Participant>,
  approvers: List<Approver>,
  onApprove: () -> Unit,
  onRevoke: () -> Unit,
  onCancel: () -> Unit,
  onFinalize: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    Row(modifier = Modifier.padding(start = 16.dp)) {
      WSpacer()
      HSpacer(48.dp)
      Approvers(
        modifier = Modifier.padding(top = 12.dp),
        approvers = approvers
      )
      WSpacer()
      HSpacer(16.dp)
      IconAction(
        iconResId = R.drawable.ic_close_24,
        iconTint = AppTheme.colors.errorPrimary,
        contentDescription = null,
        onClick = onCancel
      )
    }
    if (approvers.isNotEmpty()) {
      VSpacer(12.dp)
      Text(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        text = stringResource(
          R.string.merge_request_approvers_count,
          remember(approvers) { approvers.filter { it.isApproved } }.size,
          approvers.size
        ),
        style = AppTheme.typography.caption,
        color = AppTheme.colors.contentTertiary,
        textAlign = TextAlign.Center
      )
    }
    VSpacer(12.dp)
    Actions(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      status = mergeRequest?.status,
      isCurrentUserApproved = isCurrentUserApproved,
      mergeRequestInProgress = mergeRequestInProgress,
      onRevoke = onRevoke,
      onApprove = onApprove,
      onFinalize = onFinalize
    )
    VSpacer(14.dp)
    val initiator = remember(participants) {
      participants.firstOrNull { it.id.value == mergeRequest?.initiatorId?.value }
    }
    if (initiator != null) {
      Text(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        text = stringResource(
          R.string.merge_request_opened_by_into_branch,
          initiator.displayName.orEmpty()
        ),
        style = AppTheme.typography.caption,
        color = AppTheme.colors.contentTertiary
      )
    }
    VSpacer(16.dp)
  }
}

@Composable
private fun Approvers(
  approvers: List<Approver>,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.horizontalScroll(rememberScrollState()),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    approvers.forEach { approver ->
      Approver(
        approver = approver
      )
    }
  }
}

@Composable
private fun Approver(
  approver: Approver,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(contentAlignment = Alignment.BottomEnd) {
      Avatar(
        size = 40.dp,
        photoUrl = approver.photoUrl,
        fallbackInitial = "?"
      )
      DoneBadge(
        modifier = Modifier.size(18.dp),
        isApproved = approver.isApproved
      )
    }
    if (approver.displayName != null) {
      Text(
        text = approver.displayName,
        style = AppTheme.typography.caption,
        color = AppTheme.colors.contentTertiary
      )
    }
  }
}

@Composable
private fun DoneBadge(
  isApproved: Boolean,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .background(
        AppTheme.colors.cardPrimary,
        CircleShape
      )
      .padding(2.dp)
      .background(
        if (isApproved) AppTheme.colors.successPrimary else AppTheme.colors.contentGoldPrimary,
        CircleShape
      )
  )
}

@Composable
private fun Actions(
  status: Branch.MergeRequest.Status?,
  isCurrentUserApproved: Boolean,
  onRevoke: () -> Unit,
  onApprove: () -> Unit,
  onFinalize: () -> Unit,
  mergeRequestInProgress: Boolean,
  modifier: Modifier = Modifier
) {
  val crossfadeSpec = AppTheme.motion.mediumTween<Float>()
  RevealSplitRow(
    modifier = modifier,
    animationSpec = crossfadeSpec,
    rightVisible = status == Branch.MergeRequest.Status.ReadyToMerge,
    left = {
      AnimatedContent(
        targetState = isCurrentUserApproved,
        transitionSpec = {
          fadeIn(crossfadeSpec) togetherWith fadeOut(crossfadeSpec) using SizeTransform(
            clip = false
          ) { _, _ -> snap() }
        }
      ) { approved ->
        if (approved) {
          SecondaryTextButton(
            text = stringResource(R.string.merge_request_revoke_approval_button),
            style = ButtonStyle.Success,
            showLoading = mergeRequestInProgress,
            onClick = onRevoke
          )
        } else {
          PrimaryButton(
            text = stringResource(R.string.merge_request_approve_button),
            showLoading = mergeRequestInProgress,
            onClick = onApprove
          )
        }
      }
    },
    right = {
      PrimaryButton(
        text = stringResource(R.string.merge_request_merge_button),
        style = ButtonStyle.Success,
        enabled = status == Branch.MergeRequest.Status.ReadyToMerge,
        showLoading = mergeRequestInProgress,
        onClick = onFinalize
      )
    }
  )
}

@Preview
@Composable
private fun MergeRequestButtonPreviewLight(
  @PreviewParameter(MergeRequestCardPreviewProvider::class)
  preview: MergeRequestCardPreview
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    MergeRequestBanner(
      modifier = Modifier
        .padding(16.dp)
        .background(AppTheme.colors.cardPrimary, RoundedCornerShape(12.dp)),
      mergeRequest = preview.mergeRequest,
      isCurrentUserApproved = preview.isCurrentUserApproved,
      mergeRequestInProgress = false,
      participants = preview.participants,
      approvers = preview.approvers,
      onApprove = {},
      onRevoke = {},
      onCancel = {},
      onFinalize = {}
    )
  }
}

@Immutable
private data class MergeRequestCardPreview(
  val mergeRequest: Branch.MergeRequest,
  val initiatorName: String,
  val isCurrentUserApproved: Boolean,
  val participants: List<Participant>,
  val approvers: List<Approver>
)

@Immutable
private class MergeRequestCardPreviewProvider : PreviewParameterProvider<MergeRequestCardPreview> {
  val firstParticipantId = randomUuid()
  val secondParticipantId = randomUuid()
  val thirdParticipantId = randomUuid()

  private val initiatorName = "Сергей Лановой"
  private val mergeRequest = Branch.MergeRequest(
    status = Branch.MergeRequest.Status.Open,
    initiatorId = UserId(firstParticipantId),
    requestedAt = 0,
    approvedByIds = setOf(),
    mergedAt = 0,
    mergedIntoBranchId = Branch.Id(randomUuid())
  )

  private val participants = listOf(
    Participant(
      id = Participant.Id(firstParticipantId),
      displayName = "first approver",
      photoUrl = null
    ),
    Participant(
      id = Participant.Id(secondParticipantId),
      displayName = "second approver",
      photoUrl = null
    ),
    Participant(
      id = Participant.Id(thirdParticipantId),
      displayName = "third approver",
      photoUrl = null
    )
  )

  private val approvers = listOf(
    Approver(
      userId = UserId(firstParticipantId),
      displayName = "first approver",
      photoUrl = null,
      isApproved = false
    ),
    Approver(
      userId = UserId(thirdParticipantId),
      displayName = "third approver",
      photoUrl = null,
      isApproved = false
    )
  )
  override val values = sequenceOf(
    MergeRequestCardPreview(
      mergeRequest = mergeRequest.copy(status = Branch.MergeRequest.Status.Open),
      initiatorName = initiatorName,
      participants = participants,
      approvers = emptyList(),
      isCurrentUserApproved = false
    ),
    MergeRequestCardPreview(
      mergeRequest = mergeRequest.copy(status = Branch.MergeRequest.Status.Open),
      initiatorName = initiatorName,
      participants = participants,
      approvers = approvers,
      isCurrentUserApproved = false
    ),
    MergeRequestCardPreview(
      mergeRequest = mergeRequest,
      initiatorName = initiatorName,
      participants = participants,
      approvers = approvers.mapIndexed { index, approver ->
        approver.copy(isApproved = index % 2 == 0)
      },
      isCurrentUserApproved = true
    ),
    MergeRequestCardPreview(
      mergeRequest = mergeRequest.copy(status = Branch.MergeRequest.Status.ReadyToMerge),
      initiatorName = initiatorName,
      participants = participants,
      approvers = approvers.mapIndexed { index, approver ->
        approver.copy(isApproved = true)
      },
      isCurrentUserApproved = true
    )
  )
}
