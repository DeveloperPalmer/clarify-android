package ru.sla.clarify.feature.chat.group.thread.data.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.lib.google.firestore.entity.CommitNM
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

@Suppress("LongParameterList") // сигнатура строки ChatCommit
internal fun mapToCommit(
  id: String,
  senderId: String,
  type: String,
  text: String,
  invitedUid: String?,
  colorHex: String,
  timestamp: Long,
  isSelf: Boolean,
  status: String
): Commit {
  val localTimestamp = Instant
    .ofEpochSecond(timestamp)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()
  return if (type == CommitNM.Type.InviteParticipant.value) {
    Commit.InviteParticipant(
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
      colorHex = colorHex,
      timestamp = localTimestamp,
      isSelf = isSelf,
      status = Commit.Status.fromValue(status)
    )
  }
}

internal fun generateColorHex(): String {
  val rgb = Random.nextInt(0x1000000)
  return "#$OPAQUE_ALPHA_HEX${rgb.toString(radix = 16).padStart(6, '0').uppercase()}"
}

private const val OPAQUE_ALPHA_HEX = "FF"
