package ru.sla.clarify.feature.chat.ui.screen.list

import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.core.ui.event.ViewEvent
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
