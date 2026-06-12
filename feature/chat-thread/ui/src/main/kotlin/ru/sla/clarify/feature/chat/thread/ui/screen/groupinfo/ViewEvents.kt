package ru.sla.clarify.feature.chat.thread.ui.screen.groupinfo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.sla.clarify.core.ui.event.ViewEventHostScope
import ru.sla.clarify.feature.chat.thread.ui.entity.GroupMember
import ru.sla.clarify.feature.chat.thread.ui.screen.invitemembers.InviteMembersContent
import ru.sla.clarify.uikit.component.bottomsheet.ModalBottomSheet
import ru.sla.clarify.uikit.component.button.PrimaryButtonSmall
import ru.sla.clarify.uikit.component.button.PrimaryTextButtonSmall
import ru.sla.clarify.uikit.component.textfield.SecondaryTextField
import ru.sla.clarify.uikit.event.Dialog
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.resRef

internal fun showRenameGroupDialog(currentName: String) = ScreenViewEvent<ViewState, ViewIntents> { _, intents ->
  object : ViewEvent.Content() {
    @Composable
    override fun ViewEventHostScope.Content() {
      var inputValue by rememberSaveable { mutableStateOf(currentName) }
      val trimmedName = inputValue.trim()
      BackHandler { dismissEventPresentation() }
      AlertDialog(
        containerColor = AppTheme.colors.cardSecondary,
        onDismissRequest = { dismissEventPresentation() },
        title = {
          Text(
            text = stringResource(R.string.group_info_rename_dialog_title),
            color = AppTheme.colors.contentPrimary,
            style = AppTheme.typography.headline3
          )
        },
        text = {
          SecondaryTextField(
            modifier = Modifier.fillMaxWidth(),
            value = inputValue,
            onValueChange = { inputValue = it },
            placeholder = resRef(R.string.conversation_new_group_name_placeholder)
          )
        },
        dismissButton = {
          PrimaryTextButtonSmall(
            onClick = { dismissEventPresentation() },
            text = stringResource(R.string.action_cancel)
          )
        },
        confirmButton = {
          PrimaryButtonSmall(
            onClick = {
              intents.confirmRename(trimmedName)
              dismissEventPresentation()
            },
            enabled = trimmedName.isNotEmpty() && trimmedName != currentName,
            text = stringResource(R.string.group_info_rename)
          )
        }
      )
    }
  }
}

internal fun showInviteMembersSheet(): ScreenViewEvent<ViewState, ViewIntents> {
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
          InviteMembersContent(
            modifier = Modifier
              .fillMaxWidth()
              .fillMaxHeight(fraction = 0.9f),
            query = state.searchQuery,
            candidates = state.searchResults,
            selected = state.selectedCandidates,
            isLimitReached = state.isInviteLimitReached,
            onQueryChange = intents.changeSearchQuery,
            onClose = { dismissEventPresentation() },
            onToggleCandidate = intents.toggleCandidate,
            onConfirm = {
              intents.confirmInvite()
              dismissEventPresentation()
            }
          )
        }
      }
    }
  }
}

internal fun showRemoveMemberDialog(member: GroupMember) = ScreenViewEvent<ViewState, ViewIntents> { _, intents ->
  Dialog.Decision(
    isDestructive = true,
    title = resRef(R.string.group_info_remove_confirm_title),
    text = resRef(R.string.group_info_remove_confirm_text, member.displayName),
    primaryActionTitle = resRef(R.string.group_info_remove_member),
    secondaryActionTitle = resRef(R.string.action_cancel),
    primaryAction = { intents.confirmRemoveMember(member.id) },
    secondaryAction = { }
  )
}

internal fun showLeaveGroupDialog() = ScreenViewEvent<ViewState, ViewIntents> { _, intents ->
  Dialog.Decision(
    isDestructive = true,
    title = resRef(R.string.group_info_leave_confirm_title),
    text = resRef(R.string.group_info_leave_confirm_text),
    primaryActionTitle = resRef(R.string.group_info_leave),
    secondaryActionTitle = resRef(R.string.action_cancel),
    primaryAction = intents.confirmLeaveGroup,
    secondaryAction = { }
  )
}

internal fun showDeleteGroupDialog() = ScreenViewEvent<ViewState, ViewIntents> { _, intents ->
  Dialog.Decision(
    isDestructive = true,
    title = resRef(R.string.group_info_delete_confirm_title),
    text = resRef(R.string.group_info_delete_confirm_text),
    primaryActionTitle = resRef(R.string.group_info_delete),
    secondaryActionTitle = resRef(R.string.action_cancel),
    primaryAction = intents.confirmDeleteGroup,
    secondaryAction = { }
  )
}
