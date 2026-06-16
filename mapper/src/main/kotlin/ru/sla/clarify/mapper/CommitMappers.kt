package ru.sla.clarify.mapper

import ru.sla.clarify.core.domain.date.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.entity.chat.Commit
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.resRef
import ru.sla.resourcerefs.strRef
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

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

fun formatLastCommitTimestamp(epochSeconds: Long?): TextRef? {
  if (epochSeconds == null || epochSeconds <= 0L) return null
  val zone = ZoneId.systemDefault()
  val dateTime = Instant.ofEpochSecond(epochSeconds).atZone(zone).toLocalDateTime()
  val date = dateTime.toLocalDate()
  val today = LocalDate.now(zone)
  return when (date) {
    today -> strRef(dateTime.format(TIME_FORMATTER_HOUR_MINUTE))
    today.minusDays(1) -> resRef(R.string.yesterday)
    else -> strRef(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
  }
}

fun Commit.withReadStatus(peerLastReadAt: LocalDateTime?): Commit {
  if (this !is Commit.Message || !isSelf) {
    return this
  }
  if (status != Commit.Status.Sent || peerLastReadAt == null) {
    return this
  }
  return if (timestamp.isAfter(peerLastReadAt)) this else copy(status = Commit.Status.Read)
}
