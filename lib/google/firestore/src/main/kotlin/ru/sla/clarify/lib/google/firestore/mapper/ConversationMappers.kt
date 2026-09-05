package ru.sla.clarify.lib.google.firestore.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.ConversationRecord
import ru.sla.clarify.entity.chat.Member
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM
import ru.sla.clarify.lib.google.firestore.toEpochSeconds

internal fun ConversationNM.toDomainModel(): ConversationRecord {
  return ConversationRecord(
    id = Conversation.Id(id),
    type = Conversation.Type.entries.first { it.value == type.value },
    memberIds = memberUids.map(Member::Id),
    name = name,
    ownerId = ownerUid?.let(::UserId),
    lastCommitText = lastCommitText,
    lastCommitSenderId = lastCommitSenderUid?.let(::UserId),
    lastCommitAtSeconds = lastCommitAt?.toEpochSeconds() ?: 0L
  )
}
