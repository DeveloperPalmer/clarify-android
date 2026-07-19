package ru.sla.clarify.feature.chat.branch.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.sla.clarify.core.ui.event.ViewEventHostScope
import ru.sla.clarify.feature.chat.branch.ui.screen.ViewState.DeleteCommitsParams
import ru.sla.clarify.uikit.event.DecisionDialog
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.resQtyRef
import ru.sla.resourcerefs.resRef
import ru.sla.clarify.entity.chat.Commit as DomainCommit

internal fun showDeleteMessagesDialog(commit: DomainCommit.Id?): ScreenViewEvent<ViewState, ViewIntents> {
  return ScreenViewEvent { stateFlow, intents ->
    object : ViewEvent.Content() {
      @Composable
      override fun ViewEventHostScope.Content() {
        val state by stateFlow.collectAsState()
        val peerName = state.peerName.orEmpty()
        var deleteForEveryone by remember { mutableStateOf(true) }
        val deleteIds = if (commit != null) listOf(commit) else state.selectedCommitIds
        DecisionDialog(
          title = if (deleteIds.size == 1) {
            resRef(R.string.thread_selection_delete_title_single)
          } else {
            resQtyRef(
              R.plurals.thread_selection_delete_title,
              deleteIds.size,
              deleteIds.size
            )
          },
          primaryActionTitle = resRef(R.string.conversation_delete_dialog_primary),
          secondaryActionTitle = resRef(R.string.action_cancel),
          isDestructive = true,
          onPrimaryAction = {
            val deleteCommitsParams = DeleteCommitsParams(
              ids = deleteIds,
              forEveryone = deleteForEveryone
            )
            intents.confirmDeleteCommit(deleteCommitsParams)
          }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { deleteForEveryone = !deleteForEveryone },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Checkbox(
              checked = deleteForEveryone,
              onCheckedChange = { deleteForEveryone = it },
              colors = CheckboxDefaults.colors(
                checkedColor = AppTheme.colors.contentAccentPrimary,
                uncheckedColor = AppTheme.colors.contentTertiary,
                checkmarkColor = AppTheme.colors.contentAccentSecondary
              )
            )
            Text(
              text = stringResource(R.string.thread_selection_delete_for_peer, peerName),
              color = AppTheme.colors.contentSecondary,
              style = AppTheme.typography.body2
            )
          }
        }
      }
    }
  }
}
