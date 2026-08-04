package ru.sla.clarify.mapper.data

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.lib.google.firestore.entity.ReplyCommitNM

fun ReplyCommitNM.toDomainModel(selfUserId: UserId): Commit.Reply {
  return Commit.Reply(
    id = Commit.Id(id),
    senderId = UserId(senderUid),
    isSelf = senderUid == selfUserId.value,
    text = text
  )
}
