package ru.sla.clarify.feature.chat.direct.thread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.VSpacer

@Composable
internal fun BranchesContent(
  branches: List<Branch>,
  onOpenBranch: (Branch.Id) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
  ) {
    stickyHeader {
      Text(
        modifier = Modifier
          .fillMaxWidth()
          .background(AppTheme.colors.backgroundPrimary)
          .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        text = stringResource(R.string.thread_branches_modal_sheet_title),
        color = AppTheme.colors.contentPrimary,
        style = AppTheme.typography.title1Bold
      )
    }
    if (branches.isEmpty()) {
      item { VSpacer(8.dp) }
      item {
        Text(
          modifier = Modifier.padding(horizontal = 8.dp),
          text = stringResource(R.string.thread_branches_modal_sheet_empty),
          style = AppTheme.typography.body2,
          color = AppTheme.colors.contentPrimary
        )
      }
    } else {
      item { VSpacer(8.dp) }
      itemsIndexed(
        key = { _, branch -> branch.id.value },
        items = branches
      ) { index, item ->
        BranchItem(
          modifier = Modifier.fillMaxWidth(),
          branch = item,
          onClick = { onOpenBranch(item.id) }
        )
        if (index != branches.lastIndex) {
          VSpacer(12.dp)
        }
      }
    }
  }
}
