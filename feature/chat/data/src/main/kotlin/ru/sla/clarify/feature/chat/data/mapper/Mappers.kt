package ru.sla.clarify.feature.chat.data.mapper

import com.tencent.imsdk.v2.V2TIMMessage
import ru.sla.clarify.feature.chat.domain.entity.Conversation
import ru.sla.clarify.database.chat.ChatMessage as ChatMessageDB
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage as ChatMessageDomain

object Mappers {
  fun mapToChatMessage(
    msgId: String,
    peerId: String,
    senderId: String,
    text: String,
    timestamp: Long,
    isSelf: Long,
    status: String
  ): ChatMessageDomain {
    return ChatMessageDB(
      msgId = msgId,
      peerId = peerId,
      senderId = senderId,
      text = text,
      timestamp = timestamp,
      isSelf = isSelf,
      status = status
    ).toDomain()
  }

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
      id = id,
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

internal fun ChatMessageDB.toDomain(): ChatMessageDomain {
  return ChatMessageDomain(
    msgId = msgId,
    peerId = peerId,
    senderId = senderId,
    text = text,
    timestamp = timestamp,
    isSelf = isSelf != 0L,
    status = status.toMessageStatus()
  )
}

internal fun V2TIMMessage.previewText(): String? {
  return when (elemType) {
    V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> textElem?.text
    else -> null
  }
}

fun mapStatus(rawStatus: Int): ChatMessageDomain.Status {
  return when (rawStatus) {
    V2TIMMessage.V2TIM_MSG_STATUS_SENDING -> ChatMessageDomain.Status.Sending
    V2TIMMessage.V2TIM_MSG_STATUS_SEND_SUCC -> ChatMessageDomain.Status.Sent
    V2TIMMessage.V2TIM_MSG_STATUS_SEND_FAIL -> ChatMessageDomain.Status.Failed
    else -> ChatMessageDomain.Status.Sent
  }
}

private fun String.toMessageStatus(): ChatMessageDomain.Status {
  return ChatMessageDomain.Status.entries.firstOrNull { it.name == this } ?: ChatMessageDomain.Status.Sent
}

const val MILLIS_PER_SECOND = 1000L
