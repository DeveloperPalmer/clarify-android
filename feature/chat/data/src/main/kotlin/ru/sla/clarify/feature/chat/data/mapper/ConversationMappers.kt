package ru.sla.clarify.feature.chat.data.mapper

import ru.sla.clarify.feature.chat.domain.entity.Conversation

object ConversationMappers {
  fun mapToConversation(
    id: String,
    peerId: String,
    lastMessage: String?,
    lastMessageTimestamp: Long,
    unreadCount: Long,
    peerName: String?,
    peerFaceUrl: String?
  ): Conversation {
    return Conversation(
      id = Conversation.Id(id),
      peer = Conversation.Peer(
        id = peerId,
        name = peerName,
        faceUrl = peerFaceUrl
      ),
      lastMessage = lastMessage,
      lastMessageTimestamp = lastMessageTimestamp,
      unreadCount = unreadCount
    )
  }
}
