package ru.sla.clarify.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Commit

@Suppress("LongParameterList") // сигнатура строки ChatCommit
fun mapToCommit(
  id: String,
  senderId: String,
  type: String,
  text: String,
  invitedUid: String?,
  timestamp: Long,
  isSelf: Boolean,
  status: String
): Commit {
  val localTimestamp = timestamp.toLocalDateTime()
  return when (Commit.Type.fromValue(type)) {
    Commit.Type.Text -> {
      Commit.Message(
        id = Commit.Id(id),
        senderId = UserId(senderId),
        text = text,
        timestamp = localTimestamp,
        isSelf = isSelf,
        status = Commit.Status.fromValue(status)
      )
    }
    Commit.Type.InviteMember -> {
      Commit.InviteMember(
        id = Commit.Id(id),
        senderId = UserId(senderId),
        timestamp = localTimestamp,
        isSelf = isSelf,
        status = Commit.Status.fromValue(status),
        invitedId = UserId(invitedUid.orEmpty())
      )
    }
  }
}
