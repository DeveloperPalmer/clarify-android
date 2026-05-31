package ru.sla.clarify.feature.chat.thread.ui.screen.thread

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.sla.clarify.core.ui.event.ViewEventHostScope
import ru.sla.clarify.feature.chat.thread.domain.entity.Branch
import ru.sla.clarify.feature.chat.thread.ui.screen.thread.ViewState.CreateBranchPayload
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.uikit.component.bottomsheet.ModalBottomSheet
import ru.sla.clarify.uikit.component.textfield.OutlinedTextField
import ru.sla.clarify.uikit.theme.AppTheme

internal fun showBranchCreationSheet(commit: Commit.Message) =
  ScreenViewEvent<ViewIntents> { intents ->
    object : ViewEvent.BottomSheet {

      override val sheetState = mutableStateOf<SheetState?>(null)

      @OptIn(ExperimentalMaterial3Api::class)
      @Composable
      override fun ViewEventHostScope.Content() {
        var name by remember { mutableStateOf("") }
        BackHandler { dismissEventPresentation() }
        ModalBottomSheet(
          visible = true,
          onUpdateState = { sheetState.value = it },
          onDismissRequest = { dismissEventPresentation() }
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text(
              text = stringResource(R.string.thread_create_branch_sheet_title),
              style = AppTheme.typography.title1
            )
            OutlinedTextField(
              modifier = Modifier.fillMaxWidth(),
              value = name,
              onValueChange = { name = it },
              placeholder = { Text(stringResource(R.string.thread_create_branch_sheet_name_placeholder)) }
            )
            val scope = rememberCoroutineScope()
            val isImeVisibleState = rememberUpdatedState(WindowInsets.isImeVisible)
            val keyboardController = LocalSoftwareKeyboardController.current
            Button(
              modifier = Modifier.fillMaxWidth(),
              enabled = name.isNotBlank(),
              onClick = {
                snapshotFlow { isImeVisibleState.value }
                  .distinctUntilChanged()
                  .onEach { visible ->
                    if (visible) {
                      keyboardController?.hide()
                    } else {
                      intents.createBranch(CreateBranchPayload(commit = commit, name = name.trim()))
                      dismissEventPresentation()
                    }
                  }
                  .flowOn(Dispatchers.Main.immediate)
                  .launchIn(scope)
              }
            ) {
              Text(stringResource(R.string.thread_create_branch_sheet_create_button))
            }
            Spacer(modifier = Modifier.height(8.dp))
          }
        }
      }
    }
  }

internal fun showBranchesListSheet(branches: List<Branch>) =
  ScreenViewEvent<ViewIntents> { intents ->
    object : ViewEvent.BottomSheet {

      override val sheetState = mutableStateOf<SheetState?>(null)

      @OptIn(ExperimentalMaterial3Api::class)
      @Composable
      override fun ViewEventHostScope.Content() {
        BackHandler { dismissEventPresentation() }
        ModalBottomSheet(
          visible = true,
          onUpdateState = { sheetState.value = it },
          onDismissRequest = { dismissEventPresentation() }
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = stringResource(R.string.thread_branches_list_sheet_title),
              style = AppTheme.typography.title1
            )
            if (branches.isEmpty()) {
              Text(
                text = stringResource(R.string.thread_branches_list_sheet_empty),
                style = AppTheme.typography.body2,
                color = AppTheme.colors.contentPrimary
              )
            } else {
              branches.forEach { branch ->
                TextButton(
                  modifier = Modifier.fillMaxWidth(),
                  onClick = {
                    intents.openBranch(branch.id)
                    dismissEventPresentation()
                  }
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(text = branch.name)
                    Text(text = branch.mergeRequest?.status?.name.orEmpty())
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
          }
        }
      }
    }
  }
