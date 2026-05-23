package ru.sla.clarify.feature.chat.conversation.ui.screen.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.sla.clarify.core.ui.event.ViewEventHostScope
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.uikit.event.Dialog
import ru.sla.clarify.uikit.event.DropdownMenu
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

internal fun showConversationOptions() = ScreenViewEvent<ViewIntents> { intents ->
  val items = listOf(
    ViewEvent.DropdownMenu.Item(
      title = resRef(R.string.conversation_dropdown_delete_chat),
      isDestructive = true,
      onClick = intents.showDeleteConfirmation
    )
  )
  DropdownMenu(
    items = items,
    onDismissRequest = { }
  )
}

internal fun showNewChatDialog() = ScreenViewEvent<ViewIntents> { intents ->
  object : ViewEvent.Content() {
    @Composable
    override fun ViewEventHostScope.Content() {
      var inputValue by rememberSaveable { mutableStateOf("") }
      BackHandler { dismissEventPresentation() }
      AlertDialog(
        onDismissRequest = { dismissEventPresentation() },
        title = { Text(stringResource(R.string.conversation_new_chat_dialog_title)) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.conversation_new_chat_dialog_text))
            OutlinedTextField(
              modifier = Modifier.fillMaxWidth(),
              value = inputValue,
              onValueChange = { inputValue = it },
              singleLine = true
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              intents.confirmNewChat(Peer.Id(inputValue))
              dismissEventPresentation()
            },
            enabled = inputValue.isNotBlank()
          ) {
            Text(stringResource(R.string.conversation_new_chat_dialog_start))
          }
        },
        dismissButton = {
          TextButton(onClick = { dismissEventPresentation() }) {
            Text(stringResource(R.string.action_cancel))
          }
        }
      )
    }
  }
}
