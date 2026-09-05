package ru.sla.clarify.lib.google.firestore.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Branch
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.CommitRecord
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import ru.sla.clarify.lib.google.firestore.entity.ReplyCommitNM
import ru.sla.clarify.lib.google.firestore.entity.write.ReplyCommit
import ru.sla.clarify.lib.google.firestore.toEpochNanos

internal fun CommitNM.toDomainModel(): CommitRecord {
  return CommitRecord(
    id = Commit.Id(id),
    branchId = Branch.Id(branchId),
    senderId = UserId(senderUid),
    type = Commit.Type.fromValue(type.value),
    text = text,
    invitedId = invitedUid?.let(::UserId),
    replyCommit = replyCommit?.toDomainModel(),
    createdAtNanos = createdAt?.toEpochNanos() ?: 0L,
    editedAtNanos = editedAt?.toEpochNanos()
  )
}

internal fun ReplyCommitNM.toDomainModel(): CommitRecord.Reply {
  return CommitRecord.Reply(
    id = Commit.Id(id),
    senderId = UserId(senderUid),
    text = text
  )
}

internal fun CommitRecord.Reply.toNetworkModel(): ReplyCommit {
  return ReplyCommit(
    id = id.value,
    senderUid = senderId,
    text = text
  )
}
