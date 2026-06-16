package ru.sla.clarify.feature.chat.conversation.data.mapper

import ru.sla.clarify.core.domain.date.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.resRef
import ru.sla.resourcerefs.strRef
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

internal fun mapToConversation(
  id: String,
  type: String,
  lastCommit: String?,
  lastCommitTimestamp: Long,
  unreadCount: Long,
  peerId: String,
  peerDisplayName: String,
  peerPhotoUrl: String?
): Conversation {
  return when (ConversationNM.Type.entries.first { it.value == type }) {
    ConversationNM.Type.Direct -> {
      Conversation.Direct(
        id = Conversation.Id(id),
        peer = Peer(
          id = Peer.Id(peerId),
          displayName = peerDisplayName,
          photoUrl = peerPhotoUrl
        ),
        lastCommit = lastCommit,
        lastCommitAt = formatLastCommitTimestamp(lastCommitTimestamp),
        lastCommitTimestamp = lastCommitTimestamp,
        unreadCount = unreadCount
      )
    }
    ConversationNM.Type.Group -> {
      // TODO: @sla Conversation. Remove mapToGroup. Add Group mapper here instead of throw error
      error("unexpected conversation type: $type")
    }
  }
}

// TODO: @sla Conversation. Remove "Suppress" when remove UnusedParameter
@Suppress("UnusedParameter")
internal fun mapToGroup(
  id: String,
  name: String?,
  // TODO: @sla Conversation. Remove unused "ownerUid"
  ownerUid: String?,
  lastCommit: String?,
  // TODO: @sla Conversation. Remove unused "fake"
  fake: String?,
  lastCommitTimestamp: Long,
  unreadCount: Long,
  // TODO: @sla Conversation. Remove unused "memberCount"
  memberCount: Long,
  lastCommitSenderDisplayName: String?
): Conversation.Group {
  return Conversation.Group(
    id = Conversation.Id(id),
    name = name.orEmpty(),
    lastCommit = lastCommit,
    lastCommitSenderName = lastCommitSenderDisplayName,
    lastCommitAt = formatLastCommitTimestamp(lastCommitTimestamp),
    lastCommitTimestamp = lastCommitTimestamp,
    unreadCount = unreadCount
  )
}

internal fun mapToUser(
  id: String,
  email: String,
  displayName: String,
  photoUrl: String?
): User {
  return User(
    id = UserId(id),
    email = Email(email),
    displayName = displayName,
    photoUrl = photoUrl
  )
}

private fun formatLastCommitTimestamp(epochSeconds: Long?): TextRef? {
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
