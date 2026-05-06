package ru.sla.clarify.feature.chat.data.mapper

import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMMessage
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.feature.chat.domain.entity.Conversation

internal fun V2TIMConversation.toDomain(): Conversation? {
  if (type != V2TIMConversation.V2TIM_C2C) return null
  val peerId = userID ?: return null
  val last = lastMessage
  return Conversation(
    peerUserId = peerId,
    peerNickname = showName,
    peerFaceUrl = faceUrl,
    lastMessage = last?.previewText(),
    lastMessageTimestamp = last?.timestamp?.let { it * MILLIS_PER_SECOND } ?: 0L,
    unreadCount = unreadCount
  )
}

internal fun V2TIMMessage.toDomain(): ChatMessage? {
  val text = textElem?.text ?: return null
  val senderId = sender ?: return null
  val peerId = if (isSelf) userID ?: return null else senderId
  return ChatMessage(
    msgId = msgID.orEmpty(),
    peerUserId = peerId,
    senderId = senderId,
    text = text,
    timestamp = timestamp * MILLIS_PER_SECOND,
    isSelf = isSelf,
    status = mapStatus(status)
  )
}

private fun V2TIMMessage.previewText(): String? {
  return when (elemType) {
    V2TIMMessage.V2TIM_ELEM_TYPE_TEXT -> textElem?.text
    else -> null
  }
}

private fun mapStatus(rawStatus: Int): ChatMessage.Status {
  return when (rawStatus) {
    V2TIMMessage.V2TIM_MSG_STATUS_SENDING -> ChatMessage.Status.Sending
    V2TIMMessage.V2TIM_MSG_STATUS_SEND_SUCC -> ChatMessage.Status.Sent
    V2TIMMessage.V2TIM_MSG_STATUS_SEND_FAIL -> ChatMessage.Status.Failed
    else -> ChatMessage.Status.Sent
  }
}

private const val MILLIS_PER_SECOND = 1000L
