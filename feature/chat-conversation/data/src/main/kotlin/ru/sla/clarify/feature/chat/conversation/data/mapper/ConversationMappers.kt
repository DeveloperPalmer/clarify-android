package ru.sla.clarify.feature.chat.conversation.data.mapper

import app.cash.sqldelight.Query
import ru.sla.clarify.core.domain.date.TIME_FORMATTER_HOUR_MINUTE
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.core.resources.R
import ru.sla.clarify.database.chat.ChatConversationQueries
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM.Type
import ru.sla.resourcerefs.TextRef
import ru.sla.resourcerefs.resRef
import ru.sla.resourcerefs.strRef
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

internal fun ChatConversationQueries.selectAll(userId: UserId): Query<Conversation> {
  return selectAllWithPeer(currentUserId = userId.value) {
      id,
      type,
      lastCommit,
      lastCommitTimestamp,
      unreadCount,
      peerId,
      peerDisplayName,
      peerPhotoUrl
    ->
    when (type) {
      Type.Direct.value -> {
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
      else -> error("unexpected conversation type: $type")
    }
  }
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

private fun formatLastCommitTimestamp(epochSeconds: Long): TextRef {
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
