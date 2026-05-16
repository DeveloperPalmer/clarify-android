package ru.sla.clarify.feature.chat.conversation.data.mapper

import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer

object ConversationMappers {
  fun mapToConversation(
    id: String,
    peerId: String,
    lastMessage: String?,
    lastMessageTimestamp: Long,
    unreadCount: Long,
    peerFaceUrl: String?
  ): Conversation {
    return Conversation(
      id = Conversation.Id(id),
      peer = Peer(
        id = Peer.Id(peerId),
        faceUrl = peerFaceUrl
      ),
      lastMessage = lastMessage,
      lastMessageTimestamp = lastMessageTimestamp,
      unreadCount = unreadCount
    )
  }
}
