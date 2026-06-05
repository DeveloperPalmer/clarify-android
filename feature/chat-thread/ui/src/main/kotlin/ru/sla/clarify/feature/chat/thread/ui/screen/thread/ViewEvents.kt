package ru.sla.clarify.feature.chat.thread.ui.screen.thread

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.sla.clarify.core.ui.event.ViewEventHostScope
import ru.sla.clarify.feature.chat.thread.ui.components.BranchItem
import ru.sla.clarify.feature.chat.thread.ui.entity.Commit
import ru.sla.clarify.feature.chat.thread.ui.screen.thread.ViewState.CreateBranchPayload
import ru.sla.clarify.uikit.component.bottomsheet.ModalBottomSheet
import ru.sla.clarify.uikit.component.button.PrimaryButton
import ru.sla.clarify.uikit.component.textfield.PrimaryTextField
import ru.sla.clarify.uikit.keyboard.rememberKeyboardController
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.VSpacer
import ru.sla.resourcerefs.resRef

internal fun showBranchCreationModalSheet(commit: Commit.Message): ScreenViewEvent<ViewState, ViewIntents> {
  return ScreenViewEvent { _, intents ->
    object : ViewEvent.BottomSheet {
      override val sheetState = mutableStateOf<SheetState?>(null)

      @Composable
      override fun ViewEventHostScope.Content() {
        ModalBottomSheet(
          visible = true,
          onUpdateState = { sheetState.value = it },
          onDismissRequest = { dismissEventPresentation() },
          properties = ModalBottomSheetProperties()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp)
          ) {
            val scope = rememberCoroutineScope()
            var branchName by remember { mutableStateOf("") }
            val keyboardController = rememberKeyboardController()
            Text(
              text = stringResource(R.string.thread_create_branch_sheet_title),
              color = AppTheme.colors.contentPrimary,
              style = AppTheme.typography.title1Bold
            )
            VSpacer(6.dp)
            Text(
              text = stringResource(R.string.thread_create_branch_sheet_description),
              color = AppTheme.colors.contentSecondary,
              style = AppTheme.typography.body2
            )
            VSpacer(12.dp)
            PrimaryTextField(
              modifier = Modifier.fillMaxWidth(),
              value = branchName,
              onValueChange = { branchName = it },
              placeholder = resRef(R.string.thread_create_branch_sheet_field_name_placeholder)
            )
            VSpacer(12.dp)
            PrimaryButton(
              modifier = Modifier.fillMaxWidth(),
              enabled = branchName.isNotBlank(),
              text = stringResource(R.string.thread_create_branch_sheet_create_button),
              onClick = {
                scope.launch {
                  val payload = CreateBranchPayload(
                    commit = commit,
                    name = branchName.trim()
                  )
                  keyboardController.awaitHide()
                  sheetState.value?.hide()
                  intents.confirmCreateBranch(payload)
                  dismissEventPresentation()
                }
              }
            )
          }
        }
      }
    }
  }
}

internal fun showBranchesModalSheet(): ScreenViewEvent<ViewState, ViewIntents> {
  return ScreenViewEvent { stateFlow, intents ->
    object : ViewEvent.BottomSheet {
      override val sheetState = mutableStateOf<SheetState?>(null)

      @OptIn(ExperimentalMaterial3Api::class)
      @Composable
      override fun ViewEventHostScope.Content() {
        val state by stateFlow.collectAsState()
        BackHandler { dismissEventPresentation() }
        ModalBottomSheet(
          visible = true,
          onUpdateState = { sheetState.value = it },
          onDismissRequest = { dismissEventPresentation() }
        ) {
          LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
          ) {
            item {
              Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = stringResource(R.string.thread_branches_modal_sheet_title),
                color = AppTheme.colors.contentPrimary,
                style = AppTheme.typography.title1Bold
              )
              VSpacer(16.dp)
            }
            if (state.branches.isEmpty()) {
              item {
                Text(
                  modifier = Modifier.padding(horizontal = 8.dp),
                  text = stringResource(R.string.thread_branches_modal_sheet_empty),
                  style = AppTheme.typography.body2,
                  color = AppTheme.colors.contentPrimary
                )
              }
            } else {
              itemsIndexed(
                key = { _, branch -> branch.id.value },
                items = state.branches
              ) { index, item ->
                BranchItem(
                  modifier = Modifier.fillMaxWidth(),
                  branch = item,
                  onClick = {
                    intents.openBranch(item.id)
                    dismissEventPresentation()
                  }
                )
                if (index != state.branches.lastIndex) {
                  VSpacer(12.dp)
                }
              }
            }
          }
        }
      }
    }
  }
}
