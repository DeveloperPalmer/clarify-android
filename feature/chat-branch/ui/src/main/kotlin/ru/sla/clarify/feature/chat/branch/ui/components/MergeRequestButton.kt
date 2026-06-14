package ru.sla.clarify.feature.chat.branch.ui.components

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.entity.chat.Branch.MergeRequest.Status
import ru.sla.clarify.uikit.animation.LocalSharedTransitionScope
import ru.sla.clarify.uikit.animation.SharedContainer
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme

@Composable
internal fun MergeRequestButton(
  visible: Boolean,
  inProgress: Boolean,
  status: Status?,
  onOpenMergeRequest: () -> Unit,
  modifier: Modifier = Modifier
) {
  SharedContainer(
    modifier = modifier,
    key = MERGE_REQUEST_MOTION_KEY,
    visible = visible,
    restingCorner = MERGE_REQUEST_BUTTON_CORNER,
    morphedCorner = MERGE_REQUEST_CARD_CORNER
  ) {
    Box(
      modifier = Modifier.sharedSurface(
        elevation = AppTheme.elevation.small,
        color = AppTheme.colors.cardPrimary,
        enabled = status != Status.Merged,
        onClick = onOpenMergeRequest
      )
    ) {
      Row(
        modifier = Modifier
          .revealContent(MERGE_REQUEST_BUTTON_REVEAL_WINDOW)
          .padding(8.dp, 8.dp, 12.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        LeadingIcon(
          status = status,
          inProgress = inProgress
        )
        Text(
          text = status.text(),
          style = AppTheme.typography.title3Bold,
          color = AppTheme.colors.contentPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun LeadingIcon(
  status: Status?,
  inProgress: Boolean
) {
  val color = animateColorAsState(
    label = "merge-button-tone",
    targetValue = when (status) {
      null -> AppTheme.colors.contentAccentPrimary
      Status.Open -> AppTheme.colors.contentAccentPrimary
      Status.ReadyToMerge -> AppTheme.colors.successPrimary
      Status.Merged -> AppTheme.colors.contentSecondary
    }
  )
  if (inProgress) {
    CircularProgressIndicator(
      modifier = Modifier
        .size(24.dp)
        .background(
          shape = CircleShape,
          color = AppTheme.colors.backgroundSecondary
        )
        .padding(4.dp),
      strokeWidth = 1.2.dp,
      color = AppTheme.colors.contentAccentPrimary
    )
  } else {
    when (status) {
      null -> {
        Box(
          modifier = Modifier
            .size(24.dp)
            .background(
              shape = CircleShape,
              color = AppTheme.colors.cardSecondary
            )
            .padding(4.dp)
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_plus_24),
            tint = { color.value },
            contentDescription = null
          )
        }
      }
      Status.Open -> {
        Box(
          modifier = Modifier
            .size(24.dp)
            .background(
              shape = CircleShape,
              color = AppTheme.colors.cardSecondary
            )
            .padding(4.dp)
        ) {
          Icon(
            modifier = Modifier.offset(x = 1.dp),
            painter = painterResource(R.drawable.ic_git_branch_24),
            tint = { color.value },
            contentDescription = null
          )
        }
      }
      Status.ReadyToMerge -> {
        Box(
          modifier = Modifier
            .size(24.dp)
            .background(
              shape = CircleShape,
              color = AppTheme.colors.cardSecondary
            )
            .padding(4.dp)
        ) {
          Icon(
            modifier = Modifier.offset(x = 1.dp),
            painter = painterResource(R.drawable.ic_git_merge_24),
            tint = { color.value },
            contentDescription = null
          )
        }
      }
      Status.Merged -> {
        Icon(
          modifier = Modifier
            .size(24.dp)
            .background(
              shape = CircleShape,
              color = AppTheme.colors.cardSecondary
            )
            .padding(4.dp),
          painter = painterResource(R.drawable.ic_check_24),
          tint = { color.value },
          contentDescription = null
        )
      }
    }
  }
}

@Composable
private fun Status?.text(): String {
  return when (this) {
    null -> stringResource(R.string.merge_request_open_button)
    Status.Open -> stringResource(R.string.merge_request_opened_button)
    Status.ReadyToMerge -> stringResource(R.string.merge_request_ready_button)
    Status.Merged -> stringResource(R.string.merge_request_merged_button)
  }
}

@Preview
@Composable
private fun MergeRequestButtonPreviewLight(
  @PreviewParameter(StatusPreviewProvider::class)
  statusPreview: StatusPreview
) {
  AppTheme(currentTheme = ColorTheme.Light) {
    SharedTransitionLayout {
      CompositionLocalProvider(LocalSharedTransitionScope provides this) {
        MergeRequestButton(
          visible = true,
          inProgress = statusPreview.inProgress,
          status = statusPreview.status,
          onOpenMergeRequest = {}
        )
      }
    }
  }
}

@Preview
@Composable
private fun MergeRequestButtonPreviewDark(
  @PreviewParameter(StatusPreviewProvider::class)
  statusPreview: StatusPreview
) {
  AppTheme(currentTheme = ColorTheme.Dark) {
    SharedTransitionLayout {
      CompositionLocalProvider(LocalSharedTransitionScope provides this) {
        MergeRequestButton(
          visible = true,
          inProgress = statusPreview.inProgress,
          status = statusPreview.status,
          onOpenMergeRequest = {}
        )
      }
    }
  }
}

@Immutable
private data class StatusPreview(
  val status: Status?,
  val inProgress: Boolean
)

@Immutable
private class StatusPreviewProvider : PreviewParameterProvider<StatusPreview> {
  override val values = sequenceOf(
    StatusPreview(
      status = null,
      inProgress = false
    ),
    StatusPreview(
      status = null,
      inProgress = true
    ),
    StatusPreview(
      status = Status.Open,
      inProgress = false
    ),
    StatusPreview(
      status = Status.Open,
      inProgress = true
    ),
    StatusPreview(
      status = Status.ReadyToMerge,
      inProgress = false
    ),
    StatusPreview(
      status = Status.ReadyToMerge,
      inProgress = true
    ),
    StatusPreview(
      status = Status.Merged,
      inProgress = false
    ),
    StatusPreview(
      status = Status.Merged,
      inProgress = true
    )
  )
}

internal const val MERGE_REQUEST_MOTION_KEY: String = "merge-request-motion-key"

internal val MERGE_REQUEST_BUTTON_CORNER: Dp = 19.dp
internal val MERGE_REQUEST_CARD_CORNER: Dp = 16.dp
internal val MERGE_REQUEST_BUTTON_REVEAL_WINDOW: ClosedFloatingPointRange<Float> = 0f..0.1f
internal val MERGE_REQUEST_CARD_REVEAL_WINDOW: ClosedFloatingPointRange<Float> = 0.2f..1f
