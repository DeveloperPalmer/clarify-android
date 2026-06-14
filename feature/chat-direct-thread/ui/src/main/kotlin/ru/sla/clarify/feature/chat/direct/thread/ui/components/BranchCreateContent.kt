package ru.sla.clarify.feature.chat.direct.thread.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.CreateBranch
import ru.sla.clarify.feature.chat.direct.thread.ui.entity.CreateBranchError
import ru.sla.clarify.feature.chat.direct.thread.ui.screen.thread.ViewState.CreateBranchPayload
import ru.sla.clarify.feature.entity.chat.Branch
import ru.sla.clarify.uikit.component.button.PrimaryButton
import ru.sla.clarify.uikit.component.chat.Commit
import ru.sla.clarify.uikit.component.textfield.PrimaryTextField
import ru.sla.clarify.uikit.keyboard.rememberKeyboardController
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.VSpacer
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.resRef

@Composable
internal fun BranchCreateContent(
  commit: Commit.Message,
  branches: List<Branch>,
  createBranchError: CreateBranchError?,
  onClearCreateBranchError: () -> Unit,
  onSuccess: suspend (CreateBranchPayload) -> Unit,
  onFailure: suspend (CreateBranchError) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
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
      errorText = createBranchError?.message(),
      onValueChange = {
        branchName = it
        onClearCreateBranchError()
      },
      placeholder = resRef(R.string.thread_create_branch_sheet_field_name_placeholder)
    )
    VSpacer(12.dp)
    PrimaryButton(
      modifier = Modifier.fillMaxWidth(),
      enabled = branchName.isNotBlank(),
      text = stringResource(R.string.thread_create_branch_sheet_create_button),
      onClick = {
        scope.launch {
          CreateBranch(
            name = branchName,
            branches = branches
          ).onLeft { errors ->
            onFailure(errors.first())
          }.onRight { validated ->
            keyboardController.awaitHide()
            onSuccess(CreateBranchPayload(commit, validated.name))
          }
        }
      }
    )
  }
}

private fun CreateBranchError.message(): TextRef {
  return when (this) {
    is CreateBranchError.Empty -> {
      resRef(R.string.thread_create_branch_error_empty)
    }
    is CreateBranchError.Invalid -> {
      resRef(R.string.thread_create_branch_error_invalid)
    }
    is CreateBranchError.AlreadyExist -> {
      resRef(R.string.thread_create_branch_error_already_exists)
    }
    is CreateBranchError.RangeExceeded -> {
      resRef(R.string.thread_create_branch_error_max_length, max)
    }
  }
}
