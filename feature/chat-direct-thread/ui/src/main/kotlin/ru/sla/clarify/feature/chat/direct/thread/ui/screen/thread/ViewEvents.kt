package ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.sla.clarify.core.ui.event.ViewEventHostScope
import ru.sla.clarify.feature.chat.direct.thread.ui.components.BranchCreateContent
import ru.sla.clarify.feature.chat.direct.thread.ui.components.BranchesContent
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.Commit
import ru.sla.clarify.uikit.component.bottomsheet.ModalBottomSheet

internal fun showBranchCreationModalSheet(commit: Commit.Message): ScreenViewEvent<ViewState, ViewIntents> {
  return ScreenViewEvent { stateFlow, intents ->
    object : ViewEvent.BottomSheet {
      override val sheetState = mutableStateOf<SheetState?>(null)

      @Composable
      override fun ViewEventHostScope.Content() {
        BackHandler {
          intents.clearCreateBranchError()
          dismissEventPresentation()
        }
        LaunchedEffect(Unit) {
          intents.clearCreateBranchError()
        }
        ModalBottomSheet(
          visible = true,
          onUpdateState = { sheetState.value = it },
          onDismissRequest = {
            intents.clearCreateBranchError()
            dismissEventPresentation()
          },
          properties = ModalBottomSheetProperties()
        ) {
          val state by stateFlow.collectAsState()
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
        BackHandler { dismissEventPresentation() }
        ModalBottomSheet(
          visible = true,
          onUpdateState = { sheetState.value = it },
          onDismissRequest = { dismissEventPresentation() }
        ) {
          val state by stateFlow.collectAsState()
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
