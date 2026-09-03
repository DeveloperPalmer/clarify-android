package ru.sla.clarify.feature.chronology.ui.mapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ru.sla.atlas.entity.Branch
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.uikit.theme.AppColors

internal fun Branch.Status.toIconResId(): Int {
  return when (this) {
    Branch.Status.Alive -> R.drawable.ic_git_branch_24
    Branch.Status.Waiting -> R.drawable.ic_git_pull_request_24
    Branch.Status.Ready -> R.drawable.ic_git_merge_24
    Branch.Status.Merged -> R.drawable.ic_git_merged_24
  }
}

internal fun Branch.Status.toIconTint(colors: AppColors): Color {
  return when (this) {
    Branch.Status.Alive -> colors.contentTertiary
    Branch.Status.Waiting,
    Branch.Status.Ready -> colors.contentGoldPrimary
    Branch.Status.Merged -> colors.successPrimary
  }
}

@Composable
internal fun Branch.Status.toLabel(): String {
  return when (this) {
    Branch.Status.Alive -> {
      stringResource(R.string.chronology_branch_status_alive)
    }
    Branch.Status.Waiting -> {
      stringResource(R.string.chronology_branch_status_waiting)
    }
    Branch.Status.Ready -> {
      stringResource(R.string.chronology_branch_status_ready)
    }
    Branch.Status.Merged -> {
      stringResource(R.string.chronology_branch_status_merged)
    }
  }
}
