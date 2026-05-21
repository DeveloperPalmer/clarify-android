package ru.sla.clarify.feature.chat.thread.data.mapper

import app.cash.sqldelight.Query
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.adapter.StringList
import ru.sla.clarify.database.chat.ChatConversationQueries
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Commit
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.FirestoreSchema.ConversationType
import ru.sla.clarify.lib.google.firestore.entity.FirestoreConversation
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

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
    status = Commit.Status.fromValue(status)
  )
}

internal fun ChatConversationQueries.selectById(
  conversationId: FirestoreConversation.Id,
  userId: UserId
): Query<Conversation> {
  return selectById(
    id = conversationId.value,
    mapper = {
        id: String,
        type: String,
        participantUids: StringList,
        lastCommit: String?,
        lastCommitTimestamp: Long,
        unreadCount: Long
      ->
      when (type) {
        ConversationType.Direct.value -> {
          val peerId = participantUids.first { it != userId.value }
          Conversation.Direct(
            id = Conversation.Id(id),
            peer = Peer(id = Peer.Id(peerId)),
            lastMessage = lastCommit,
            lastMessageTimestamp = lastCommitTimestamp,
            unreadCount = unreadCount
          )
        }
        else -> error("unexpected conversation type: $type")
      }
    }
  )
}

internal fun generateColorHex(): String {
  val rgb = Random.nextInt(0x1000000)
  return "#$OPAQUE_ALPHA_HEX${rgb.toString(radix = 16).padStart(6, '0').uppercase()}"
}

private const val OPAQUE_ALPHA_HEX = "FF"
