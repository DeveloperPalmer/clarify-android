package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.GroupName
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.entity.ContentLoadState
import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.sla.clarify.core.ui.event.ViewEventHostScope
import ru.sla.clarify.feature.chat.conversation.ui.screen.main.ViewState.CreateConversationTab
import ru.sla.clarify.uikit.component.button.PrimaryButtonSmall
import ru.sla.clarify.uikit.component.button.PrimaryTextButtonSmall
import ru.sla.clarify.uikit.component.tabsrow.PrimaryTabsRow
import ru.sla.clarify.uikit.component.textfield.SecondaryTextField
import ru.sla.clarify.uikit.event.Dialog
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.VSpacer
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.resRef

internal fun showDeleteConversationDialog(): ScreenViewEvent<ViewState, ViewIntents> {
  return ScreenViewEvent { _, intents ->
    Dialog.Decision(
      isDestructive = true,
      title = resRef(R.string.conversation_delete_dialog_title),
      text = resRef(R.string.conversation_delete_dialog_text),
      primaryActionTitle = resRef(R.string.conversation_delete_dialog_primary),
      secondaryActionTitle = resRef(R.string.action_cancel),
      primaryAction = intents.confirmDeleteConversation,
      secondaryAction = { }
    )
  }
}

internal fun showCreateConversationDialog(): ScreenViewEvent<ViewState, ViewIntents> {
  return ScreenViewEvent { statFlow, intents ->
    object : ViewEvent.Content() {

      override fun postDestroy() {
        if (statFlow.value.createConversationLoadState is ContentLoadState.Ready) {
          when (statFlow.value.selectedCreateConversationTab) {
            CreateConversationTab.Direct -> {
              intents.hideCreateConversation()
              intents.confirmCreateDirectConversation()
            }
            CreateConversationTab.Group -> {
              intents.hideCreateConversation()
              intents.confirmCreateGroup()
            }
          }
        }
      }

      @Composable
      override fun ViewEventHostScope.Content() {
        val state by statFlow.collectAsState()
        val keyboardController = LocalSoftwareKeyboardController.current
        val directFocusRequester = remember { FocusRequester() }
        val groupFocusRequester = remember { FocusRequester() }
        BackHandler { dismissEventPresentation() }
        LaunchedEffect(state.createConversationLoadState) {
          if (state.createConversationLoadState is ContentLoadState.Ready) {
            dismissEventPresentation()
          }
        }
        LaunchedEffect(state.selectedCreateConversationTab) {
          when (state.selectedCreateConversationTab) {
            CreateConversationTab.Direct -> {
              directFocusRequester.requestFocus()
              keyboardController?.show()
            }
            CreateConversationTab.Group -> {
              groupFocusRequester.requestFocus()
              keyboardController?.show()
            }
          }
        }
        AlertDialog(
          containerColor = AppTheme.colors.cardSecondary,
          onDismissRequest = ::dismissEventPresentation,
          title = {
            Text(
              text = stringResource(R.string.direct_conversation_title),
              color = AppTheme.colors.contentPrimary,
              style = AppTheme.typography.headline3
            )
          },
          text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              if (state.groupsAvailable) {
                PrimaryTabsRow(
                  modifier = Modifier.fillMaxWidth(),
                  tabList = CreateConversationTab.entries,
                  selectedTab = state.selectedCreateConversationTab,
                  onTabClick = { intents.changeCreateConversationTab(it) }
                )
              }
              when (state.selectedCreateConversationTab) {
                CreateConversationTab.Direct -> {
                  DirectTabContent(
                    focusRequester = directFocusRequester,
                    value = state.directEmailQuery,
                    errorText = state.directEmailError?.message(),
                    onValueChange = intents.changeEmailQuery
                  )
                }
                CreateConversationTab.Group -> {
                  GroupTabContent(
                    focusRequester = groupFocusRequester,
                    value = state.groupNameQuery,
                    errorText = state.groupNameError?.message(),
                    onValueChange = intents.changeGroupNameQuery
                  )
                }
              }
            }
          },
          dismissButton = {
            PrimaryTextButtonSmall(
              text = stringResource(R.string.action_cancel),
              enabled = state.createConversationLoadState.enabled(),
              onClick = ::dismissEventPresentation
            )
          },
          confirmButton = {
            when (state.selectedCreateConversationTab) {
              CreateConversationTab.Direct -> {
                PrimaryButtonSmall(
                  text = stringResource(R.string.direct_conversation_email_positive_button),
                  enabled = state.directEmailQuery.text.isNotBlank() &&
                    state.createConversationLoadState !is ContentLoadState.Loading,
                  showLoading = state.createConversationLoadState.showLoading(),
                  onClick = intents.validateDirectEmail
                )
              }
              CreateConversationTab.Group -> {
                PrimaryButtonSmall(
                  text = stringResource(R.string.group_conversation_positive_button),
                  enabled = state.groupNameQuery.text.isNotBlank() &&
                    state.createConversationLoadState !is ContentLoadState.Loading,
                  showLoading = state.createConversationLoadState.showLoading(),
                  onClick = intents.validateGroupName
                )
              }
            }
          }
        )
      }
    }
  }
}

@Composable
private fun DirectTabContent(
  focusRequester: FocusRequester,
  value: TextFieldValue,
  errorText: TextRef?,
  onValueChange: (TextFieldValue) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.direct_conversation_text),
      color = AppTheme.colors.contentSecondary,
      style = AppTheme.typography.body2
    )
    VSpacer(4.dp)
    SecondaryTextField(
      modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester),
      value = value,
      errorText = errorText,
      onValueChange = onValueChange,
      placeholder = resRef(R.string.direct_conversation_email_placeholder),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
  }
}

@Composable
private fun GroupTabContent(
  focusRequester: FocusRequester,
  value: TextFieldValue,
  errorText: TextRef?,
  onValueChange: (TextFieldValue) -> Unit
) {
  SecondaryTextField(
    modifier = Modifier
      .fillMaxWidth()
      .focusRequester(focusRequester),
    value = value,
    errorText = errorText,
    onValueChange = onValueChange,
    placeholder = resRef(R.string.group_conversation_name_placeholder)
  )
}

@Composable
private fun Email.Error.message(): TextRef? {
  return when (this) {
    Email.Error.Empty -> null
    Email.Error.TooShort -> resRef(R.string.direct_conversation_error_too_short)
    Email.Error.TooLong -> resRef(R.string.direct_conversation_error_too_long)
    Email.Error.Invalid -> resRef(R.string.direct_conversation_error_invalid_email)
    Email.Error.SelfEmail -> resRef(R.string.direct_conversation_error_same_email)
  }
}

@Composable
private fun GroupName.Error.message(): TextRef? {
  return when (this) {
    GroupName.Error.Empty -> null
    GroupName.Error.TooLong -> resRef(R.string.group_conversation_error_too_long)
  }
}

private fun ContentLoadState.enabled(): Boolean {
  return when (this) {
    is ContentLoadState.Error,
    is ContentLoadState.NotStarted -> true
    is ContentLoadState.Ready,
    is ContentLoadState.Loading -> false
  }
}

private fun ContentLoadState.showLoading(): Boolean {
  return when (this) {
    is ContentLoadState.Error,
    is ContentLoadState.NotStarted -> false
    is ContentLoadState.Ready,
    is ContentLoadState.Loading -> true
  }
}
