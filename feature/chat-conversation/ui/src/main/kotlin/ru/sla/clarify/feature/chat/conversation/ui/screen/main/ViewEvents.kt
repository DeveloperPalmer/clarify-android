package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.sla.clarify.core.ui.event.ViewEventHostScope
import ru.sla.clarify.feature.chat.conversation.ui.screen.main.ViewState.CreateConversationOption
import ru.sla.clarify.uikit.component.button.PrimaryButtonSmall
import ru.sla.clarify.uikit.component.button.PrimaryTextButtonSmall
import ru.sla.clarify.uikit.component.tabsrow.PrimaryTabsRow
import ru.sla.clarify.uikit.component.textfield.SecondaryTextField
import ru.sla.clarify.uikit.event.Dialog
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.clarify.uikit.theme.VSpacer
import ru.sla.resourcerefs.resRef

internal fun showDeleteConversationDialog() =
  ScreenViewEvent<ViewState, ViewIntents> { _, intents ->
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

internal fun showNewChatDialog() = ScreenViewEvent<ViewState, ViewIntents> { statFlow, intents ->
  object : ViewEvent.Content() {
    @Composable
    override fun ViewEventHostScope.Content() {
      val state by statFlow.collectAsState()
      var emailInput by rememberSaveable { mutableStateOf("") }
      var groupNameInput by rememberSaveable { mutableStateOf("") }
      val trimmedGroupName = groupNameInput.trim()
      val isValidGroupName = trimmedGroupName.isNotEmpty()

      BackHandler { dismissEventPresentation() }
      AlertDialog(
        containerColor = AppTheme.colors.cardSecondary,
        onDismissRequest = { dismissEventPresentation() },
        title = {
          Text(
            text = stringResource(R.string.conversation_new_chat_dialog_title),
            color = AppTheme.colors.contentPrimary,
            style = AppTheme.typography.headline3
          )
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryTabsRow(
              modifier = Modifier.fillMaxWidth(),
              tabList = CreateConversationOption.entries,
              selectedTab = state.selectedCreateConversationOption,
              onTabClick = { intents.changeCreateConversationTab(it) }
            )
            AnimatedContent(
              targetState = state.selectedCreateConversationOption,
              transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { tab ->
              when (tab as CreateConversationOption) {
                CreateConversationOption.Direct -> {
                  DirectTabContent(
                    value = emailInput,
                    onValueChange = { emailInput = it }
                  )
                }
                CreateConversationOption.Group -> {
                  GroupTabContent(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it }
                  )
                }
              }
            }
          }
        },
        dismissButton = {
          PrimaryTextButtonSmall(
            onClick = { dismissEventPresentation() },
            text = stringResource(R.string.action_cancel)
          )
        },
        confirmButton = {
          when (state.selectedCreateConversationOption as CreateConversationOption) {
            CreateConversationOption.Direct -> {
              PrimaryButtonSmall(
                enabled = emailInput.isNotBlank(),
                text = stringResource(R.string.conversation_new_chat_dialog_start),
                onClick = {
                  Email.validate(emailInput)
                    .onLeft { }
                    .onRight {
                      intents.confirmCreateDirect(it.value)
                      dismissEventPresentation()
                    }
                }
              )
            }
            CreateConversationOption.Group -> {
              PrimaryButtonSmall(
                onClick = {
                  intents.confirmCreateGroup(trimmedGroupName)
                  dismissEventPresentation()
                },
                enabled = isValidGroupName,
                text = stringResource(R.string.conversation_new_group_create_button)
              )
            }
          }
        }
      )
    }
  }
}

@Composable
private fun DirectTabContent(
  value: String,
  onValueChange: (String) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.conversation_new_chat_dialog_text),
      color = AppTheme.colors.contentSecondary,
      style = AppTheme.typography.body2
    )
    VSpacer(4.dp)
    SecondaryTextField(
      modifier = Modifier.fillMaxWidth(),
      value = value,
      onValueChange = onValueChange,
      placeholder = resRef(R.string.conversation_new_chat_dialog_email_placeholder),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
  }
}

@Composable
private fun GroupTabContent(
  value: String,
  onValueChange: (String) -> Unit
) {
  SecondaryTextField(
    modifier = Modifier.fillMaxWidth(),
    value = value,
    onValueChange = onValueChange,
    placeholder = resRef(R.string.conversation_new_group_name_placeholder)
  )
}
