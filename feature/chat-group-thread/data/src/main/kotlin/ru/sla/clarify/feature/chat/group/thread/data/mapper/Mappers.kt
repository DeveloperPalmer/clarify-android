package ru.sla.clarify.feature.chat.group.thread.data.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import java.time.Instant
import java.time.ZoneId

@Suppress("LongParameterList") // сигнатура строки ChatCommit
internal fun mapToCommit(
  id: String,
  senderId: String,
  type: String,
  text: String,
  invitedUid: String?,
  timestamp: Long,
  isSelf: Boolean,
  status: String
): Commit {
  val localTimestamp = Instant
    .ofEpochSecond(timestamp)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()
  return if (type == CommitNM.Type.InviteMember.value) {
    Commit.InviteMember(
      id = Commit.Id(id),
      senderId = UserId(senderId),
      timestamp = localTimestamp,
      isSelf = isSelf,
      status = Commit.Status.fromValue(status),
      invitedId = UserId(invitedUid.orEmpty())
    )
  } else {
    Commit.Message(
      id = Commit.Id(id),
      senderId = UserId(senderId),
      text = text,
      timestamp = localTimestamp,
      isSelf = isSelf,
      status = Commit.Status.fromValue(status)
    )
  }
}
