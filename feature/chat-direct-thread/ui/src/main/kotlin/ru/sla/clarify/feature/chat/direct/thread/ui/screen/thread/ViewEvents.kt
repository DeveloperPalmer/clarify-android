package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.SheetState
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
import ru.sla.clarify.feature.chat.direct.thread.ui.components.BranchCreateContent
import ru.sla.clarify.feature.chat.direct.thread.ui.components.BranchesContent
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ViewState.DeleteCommitsParams
import ru.sla.clarify.uikit.component.bottomsheet.ModalBottomSheet
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.uikit.event.DecisionDialog
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.resQtyRef
import ru.sla.resourcerefs.resRef
import ru.sla.clarify.entity.chat.Commit as DomainCommit

internal fun showBranchCreationModalSheet(commit: Commit): ScreenViewEvent<ViewState, ViewIntents> {
  return ScreenViewEvent { stateFlow, intents ->
    object : ViewEvent.BottomSheet {
      override val sheetState = mutableStateOf<SheetState?>(null)

      @Composable
      override fun ViewEventHostScope.Content() {
        val state by stateFlow.collectAsState()
        BackHandler { dismissEventPresentation() }
        ModalBottomSheet(
          visible = true,
          onUpdateState = { sheetState.value = it },
          onDismissRequest = ::dismissEventPresentation
        ) {
          BranchCreateContent(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            commit = commit,
            branches = state.branches,
            createBranchError = state.createBranchError,
            onClearCreateBranchError = intents.clearCreateBranchError,
            onFailure = intents.showCreateBranchError,
            onSuccess = { createBranchPayload ->
              sheetState.value?.hide()
              intents.confirmCreateBranch(createBranchPayload)
              dismissEventPresentation()
            }
          )
        }
      }
    }
  }
}

internal fun showBranchesModalSheet(): ScreenViewEvent<ViewState, ViewIntents> {
  return ScreenViewEvent { stateFlow, intents ->
    object : ViewEvent.BottomSheet {
      override val sheetState = mutableStateOf<SheetState?>(null)

      @Composable
      override fun ViewEventHostScope.Content() {
        val state by stateFlow.collectAsState()
        BackHandler { dismissEventPresentation() }
        ModalBottomSheet(
          visible = true,
          onUpdateState = { sheetState.value = it },
          onDismissRequest = { dismissEventPresentation() }
        ) {
          BranchesContent(
            modifier = Modifier.fillMaxWidth(),
            branches = state.branches,
            onOpenBranch = {
              intents.openBranch(it)
              dismissEventPresentation()
            }
          )
        }
      }
    }
  }
}

internal fun showDeleteMessagesDialog(commit: DomainCommit.Id?): ScreenViewEvent<ViewState, ViewIntents> {
  return ScreenViewEvent { stateFlow, intents ->
    object : ViewEvent.Content() {
      @Composable
      override fun ViewEventHostScope.Content() {
        val state by stateFlow.collectAsState()
        val peerName = state.peer?.displayName.orEmpty()
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
          // Удаление вызывают, в т.ч. не выходя из редактирования — диалог не должен гасить клавиатуру.
          keepImeVisible = true,
          onPrimaryAction = {
            val deleteCommitsParams = DeleteCommitsParams(
              ids = deleteIds,
              forEveryone = deleteForEveryone
            )
            intents.confirmDeleteCommits(deleteCommitsParams)
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
