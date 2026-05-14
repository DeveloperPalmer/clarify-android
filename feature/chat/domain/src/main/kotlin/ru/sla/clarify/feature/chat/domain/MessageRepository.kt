package ru.sla.clarify.feature.chat.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage

interface MessageRepository {
  fun peerMessages(peerId: String): Flow<ChatMessage>

  suspend fun history(
    count: Int,
    peerId: String,
    before: ChatMessage? = null
  ): List<ChatMessage>

  suspend fun send(
    text: String,
    peerId: String,
    parentId: ChatMessage.Id?
  ): ChatMessage

  suspend fun markAsRead(peerId: String)
}
