package ru.sla.clarify.feature.chat.direct.thread.data.mapper

import com.google.firebase.Timestamp
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Member
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import java.time.Instant
import java.time.LocalDateTime
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

internal fun mapToMember(
  id: String,
  displayName: String?,
  photoUrl: String?
): Member {
  return Member(
    id = Member.Id(id),
    displayName = displayName,
    photoUrl = photoUrl
  )
}

@Suppress("UnusedParameter") // Unuser only for peer
internal fun mapToPeer(
  id: String,
  email: String,
  displayName: String,
  photoUrl: String?
): Peer {
  return Peer(
    id = Peer.Id(id),
    displayName = displayName,
    photoUrl = photoUrl
  )
}

internal fun Commit.withReadStatus(peerLastReadAt: LocalDateTime?): Commit {
  if (this !is Commit.Message || !isSelf) return this
  if (status != Commit.Status.Sent || peerLastReadAt == null) return this
  return if (timestamp.isAfter(peerLastReadAt)) this else copy(status = Commit.Status.Read)
}

internal fun Timestamp.toLocalDateTime(): LocalDateTime {
  return Instant
    .ofEpochSecond(seconds)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()
}
