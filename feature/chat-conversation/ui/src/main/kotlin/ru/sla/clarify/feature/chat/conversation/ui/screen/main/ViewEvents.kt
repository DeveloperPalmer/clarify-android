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
import androidx.compose.ui.unit.dp
import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent
import ru.sla.clarify.core.ui.event.ViewEventHostScope
import ru.sla.clarify.uikit.event.Dialog
import ru.sla.clarify.uikit.event.DropdownMenu
import ru.sla.resourcerefs.strRef

internal fun showDeleteConversationDialog() = ScreenViewEvent<ViewIntents> { intents ->
  Dialog.Decision(
    title = strRef("Delete chat"),
    text = strRef("Are you sure you want to delete the conversations. This action cannot be undone."),
    primaryActionTitle = strRef("Delete"),
    secondaryActionTitle = strRef("Cancel"),
    primaryAction = intents.confirmDeleteConversation,
    secondaryAction = { }
  )
}

internal fun showConversationOptions() = ScreenViewEvent<ViewIntents> { intents ->
  val items = listOf(
    ViewEvent.DropdownMenu.Item(
      title = strRef("Delete chat"),
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
        title = { Text("New chat") },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Enter the peer userId to chat with:")
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
              intents.confirmNewChat(inputValue)
              dismissEventPresentation()
            },
            enabled = inputValue.isNotBlank()
          ) {
            Text("Start")
          }
        },
        dismissButton = {
          TextButton(onClick = { dismissEventPresentation() }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}
