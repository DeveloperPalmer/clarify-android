package ru.sla.clarify.feature.chat.thread.data.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Peer
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

data object Mappers {
  internal fun mapToConversation(
    id: String,
    peerId: String,
    lastMessage: String?,
    lastMessageTimestamp: Long,
    unreadCount: Long
  ): Conversation {
    return Conversation(
      id = Conversation.Id(id),
      peer = Peer(
        id = Peer.Id(peerId),
        faceUrl = null
      ),
      lastMessage = lastMessage,
      lastMessageTimestamp = lastMessageTimestamp,
      unreadCount = unreadCount
    )
  }

  internal fun mapToCommit(
    id: String,
    senderId: String,
    text: String,
    colorHex: String,
    timestamp: Long,
    isSelf: Boolean,
    status: String
  ): Commit.Message {
    return Commit.Message(
      id = Commit.Id(id),
      senderId = UserId(senderId),
      text = text,
      colorHex = colorHex,
      timestamp = Instant
        .ofEpochSecond(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime(),
      isSelf = isSelf,
      status = Commit.Status.valueOf(status)
    )
  }
}

internal fun generateColorHex(): String {
  val rgb = Random.nextInt(0x1000000)
  return "#$OPAQUE_ALPHA_HEX${rgb.toString(radix = 16).padStart(6, '0').uppercase()}"
}

private const val OPAQUE_ALPHA_HEX = "FF"
