package ru.sla.clarify.feature.chat.ui.screen.list

import ru.sla.clarify.core.ui.event.ScreenViewEvent
import ru.sla.clarify.feature.chat.domain.entity.Conversation
import ru.sla.clarify.uikit.event.Dialog
import ru.sla.resourcerefs.strRef

internal fun showDeleteConversationDialog(
  peerLabel: String,
  conversationId: Conversation.Id
) = ScreenViewEvent<ViewIntents> { intents ->
  Dialog.Decision(
    title = strRef("Delete chat"),
    text = strRef("Are you sure you want to delete the conversation with $peerLabel? This action cannot be undone."),
    primaryActionTitle = strRef("Delete"),
    secondaryActionTitle = strRef("Cancel"),
    primaryAction = { intents.confirmDeleteConversation(conversationId) },
    secondaryAction = { }
  )
}
