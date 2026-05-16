package ru.sla.clarify.feature.chat.data.mapper

import com.tencent.imsdk.v2.V2TIMMessage
import ru.sla.clarify.feature.chat.domain.entity.Conversation

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
      peer = Conversation.Peer(
        id = peerId,
        faceUrl = peerFaceUrl
      ),
      lastMessage = lastMessage,
      lastMessageTimestamp = lastMessageTimestamp,
      unreadCount = unreadCount
    )
  }
}

internal fun V2TIMMessage.previewText(): String? {
  return when (elemType) {
    V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> {
      textElem?.text
    }
    V2TIMMessage.V2TIM_ELEM_TYPE_CUSTOM -> {
      customElem
        ?.data
        ?.decodeToString()
        ?.toCustomMessagePayload()
        ?.text
    }
    else -> null
  }
}
