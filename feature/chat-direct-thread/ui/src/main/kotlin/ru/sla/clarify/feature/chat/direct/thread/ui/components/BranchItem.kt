package ru.sla.clarify.feature.chat.direct.thread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.feature.entity.chat.Branch
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Conversation
import ru.sla.clarify.uikit.component.UnreadCountBadge
import ru.sla.clarify.uikit.modifier.surface
import ru.sla.clarify.uikit.preview.PreviewColumn
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.ColorTheme
import ru.sla.resourcerefs.compose.resolveTextRef
import ru.sla.resourcerefs.strRef

@Composable
internal fun BranchItem(
  branch: Branch,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .surface(
        shape = AppTheme.shapes.round16,
        backgroundColor = Color.Transparent,
        onClick = onClick
      )
      .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    LeadingBlock(
      status = branch.mergeRequest?.status
    )
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = branch.name,
        color = AppTheme.colors.contentPrimary,
        style = AppTheme.typography.title2Bold
      )
      val lastCommit = branch.lastCommit
      if (lastCommit != null) {
        Text(
          text = lastCommit,
          color = AppTheme.colors.contentSecondary,
          style = AppTheme.typography.body3
        )
      }
    }
    TrailingBlock(
      date = branch.lastCommitAt?.let { resolveTextRef(it) },
      unreadCount = branch.unreadCount
    )
  }
}

@Composable
private fun LeadingBlock(
  status: Branch.MergeRequest.Status?,
  modifier: Modifier = Modifier
) {
  when (status) {
    null,
    Branch.MergeRequest.Status.Open -> {
      Icon(
        modifier = modifier
          .background(
            shape = AppTheme.shapes.round16,
            color = AppTheme.colors.backgroundAccentPrimary
          )
          .padding(8.dp),
        painter = painterResource(R.drawable.ic_git_branch_24),
        tint = AppTheme.colors.contentAccentPrimary,
        contentDescription = null
      )
    }
    Branch.MergeRequest.Status.ReadyToMerge -> {
      Icon(
        modifier = modifier
          .background(
            shape = AppTheme.shapes.round16,
            color = AppTheme.colors.backgroundAccentPrimary
          )
          .padding(8.dp),
        painter = painterResource(R.drawable.ic_git_branch_24),
        tint = AppTheme.colors.contentAccentPrimary,
        contentDescription = null
      )
    }
    Branch.MergeRequest.Status.Merged -> {
      Icon(
        modifier = modifier
          .background(
            shape = AppTheme.shapes.round16,
            color = AppTheme.colors.successSecondary
          )
          .padding(8.dp),
        painter = painterResource(R.drawable.ic_git_merged_24),
        tint = AppTheme.colors.contentSecondary,
        contentDescription = null
      )
    }
  }
}

@Composable
private fun TrailingBlock(
  date: String?,
  unreadCount: Long,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    if (date != null) {
      Text(
        text = date,
        style = AppTheme.typography.label3,
        color = AppTheme.colors.contentAccentPrimary
      )
    }
    UnreadCountBadge(
      unreadCount = unreadCount
    )
  }
}

@Preview
@Composable
private fun BranchItemPreviewLight() {
  PreviewColumn(colorTheme = ColorTheme.Light) {
    BranchItemPreviewContent()
  }
}

@Preview
@Composable
private fun BranchItemPreviewDark() {
  PreviewColumn(colorTheme = ColorTheme.Dark) {
    BranchItemPreviewContent()
  }
}

@Composable
private fun BranchItemPreviewContent() {
  BranchItem(
    modifier = Modifier.padding(horizontal = 8.dp),
    branch = previewBranch(
      name = "feature/login",
      status = null
    ),
    onClick = {}
  )
  BranchItem(
    modifier = Modifier.padding(horizontal = 8.dp),
    branch = previewBranch(
      name = "fix/crash-on-start",
      status = Branch.MergeRequest.Status.Open
    ),
    onClick = {}
  )
  BranchItem(
    modifier = Modifier.padding(horizontal = 8.dp),
    branch = previewBranch(
      name = "feature/payments",
      status = Branch.MergeRequest.Status.ReadyToMerge
    ),
    onClick = {}
  )
  BranchItem(
    modifier = Modifier.padding(horizontal = 8.dp),
    branch = previewBranch(
      name = "chore/cleanup",
      status = Branch.MergeRequest.Status.Merged
    ),
    onClick = {}
  )
}

private fun previewBranch(
  name: String,
  status: Branch.MergeRequest.Status?
): Branch {
  val author = UserId("author")
  return Branch(
    id = Branch.Id("branch-$name"),
    conversationId = Conversation.Id("conversation"),
    parentBranchId = Branch.Id("main"),
    branchedFromCommitId = Commit.Id("commit"),
    name = name,
    lastCommit = "lastCommit",
    lastCommitAt = strRef("lastCommitAt"),
    lastCommitTimestamp = 0L,
    createdAt = 0L,
    unreadCount = 0L,
    createdById = author,
    mergeRequest = status?.let {
      Branch.MergeRequest(
        status = it,
        initiatorId = author,
        requestedAt = 0L,
        approvedByIds = setOf(author),
        mergedAt = if (it == Branch.MergeRequest.Status.Merged) 0L else null,
        mergedIntoBranchId = if (it == Branch.MergeRequest.Status.Merged) Branch.Id("main") else null
      )
    }
  )
}
