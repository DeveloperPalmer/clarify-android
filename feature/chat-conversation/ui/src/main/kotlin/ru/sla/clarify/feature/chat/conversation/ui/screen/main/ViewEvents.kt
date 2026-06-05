package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.sla.clarify.core.ui.event.ViewEventHostScope
import ru.sla.clarify.uikit.component.button.PrimaryButtonSmall
import ru.sla.clarify.uikit.component.button.TextButtonSmall
import ru.sla.clarify.uikit.component.textfield.PrimaryTextField
import ru.sla.clarify.uikit.event.Dialog
import ru.sla.clarify.uikit.theme.AppTheme
import ru.sla.resourcerefs.resRef

internal fun showDeleteConversationDialog() = ScreenViewEvent<ViewState, ViewIntents> { _, intents ->
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

internal fun showNewChatDialog() = ScreenViewEvent<ViewState, ViewIntents> { _, intents ->
  object : ViewEvent.Content() {
    @Composable
    override fun ViewEventHostScope.Content() {
      var inputValue by rememberSaveable { mutableStateOf("") }
      val trimmedEmail = inputValue.trim()
      val isValidEmail = trimmedEmail.isNotEmpty() && Patterns.EMAIL_ADDRESS
        .matcher(trimmedEmail)
        .matches()
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
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = stringResource(R.string.conversation_new_chat_dialog_text),
              color = AppTheme.colors.contentSecondary,
              style = AppTheme.typography.body2
            )
            PrimaryTextField(
              modifier = Modifier.fillMaxWidth(),
              value = inputValue,
              onValueChange = { inputValue = it },
              placeholder = resRef(R.string.conversation_new_chat_dialog_email_placeholder),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
          }
        },
        dismissButton = {
          TextButtonSmall(
            onClick = { dismissEventPresentation() },
            isError = false,
            text = stringResource(R.string.action_cancel)
          )
        },
        confirmButton = {
          PrimaryButtonSmall(
            onClick = {
              intents.confirmNewChat(trimmedEmail.lowercase())
              dismissEventPresentation()
            },
            enabled = isValidEmail,
            text = stringResource(R.string.conversation_new_chat_dialog_start)
          )
        }
      )
    }
  }
}
