package ru.sla.clarify.feature.chat.conversation.data.mapper

import ru.sla.clarify.database.entity.GroupConversationRow
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.mapper.data.formatLastCommitTimestamp

// TODO: @sla Conversation. ownerId, lastCommitSenderId and memberCount of GroupConversationRow are unused
internal fun GroupConversationRow.toDomainModel(): Conversation.Group {
  return Conversation.Group(
    id = id,
    name = name.orEmpty(),
    lastCommit = lastCommit,
    lastCommitSenderName = lastCommitSenderDisplayName,
    lastCommitAt = formatLastCommitTimestamp(lastCommitTimestamp),
    lastCommitTimestamp = lastCommitTimestamp,
    unreadCount = unreadCount
  )
}
