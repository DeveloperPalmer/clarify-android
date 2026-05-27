package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import ru.sla.clarify.uikit.event.Dialog
import ru.sla.resourcerefs.resRef

internal fun showDeleteConversationDialog() = ScreenViewEvent<ViewIntents> { intents ->
  Dialog.Decision(
    title = resRef(R.string.conversation_delete_dialog_title),
    text = resRef(R.string.conversation_delete_dialog_text),
    primaryActionTitle = resRef(R.string.conversation_delete_dialog_primary),
    secondaryActionTitle = resRef(R.string.action_cancel),
    primaryAction = intents.confirmDeleteConversation,
    secondaryAction = { }
  )
}

internal fun showNewChatDialog() = ScreenViewEvent<ViewIntents> { intents ->
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
        onDismissRequest = { dismissEventPresentation() },
        title = { Text(stringResource(R.string.conversation_new_chat_dialog_title)) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = stringResource(R.string.conversation_new_chat_dialog_text)
            )
            OutlinedTextField(
              modifier = Modifier.fillMaxWidth(),
              value = inputValue,
              onValueChange = { inputValue = it },
              singleLine = true,
              placeholder = {
                Text(stringResource(R.string.conversation_new_chat_dialog_email_placeholder))
              },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              intents.confirmNewChat(trimmedEmail.lowercase())
              dismissEventPresentation()
            },
            enabled = isValidEmail
          ) {
            Text(
              text = stringResource(R.string.conversation_new_chat_dialog_start)
            )
          }
        },
        dismissButton = {
          TextButton(onClick = { dismissEventPresentation() }) {
            Text(
              text = stringResource(R.string.action_cancel)
            )
          }
        }
      )
    }
  }
}
