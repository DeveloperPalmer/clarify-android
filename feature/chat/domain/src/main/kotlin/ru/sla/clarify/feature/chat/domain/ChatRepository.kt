package ru.sla.clarify.feature.chat.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.feature.chat.domain.entity.Conversation

interface ChatRepository {
  fun getCurrentUserId(): String?

  fun subscribeOnConversations(): Flow<Unit>
  val conversations: Flow<List<Conversation>?>

  fun treadMessages(peerId: String): Flow<ChatMessage>

  suspend fun loadHistory(
    count: Int,
    peerId: String,
    before: ChatMessage? = null
  ): List<ChatMessage>

  suspend fun sendText(
    text: String,
    peerId: String,
    parentId: ChatMessage.Id?
  ): ChatMessage

  suspend fun markConversationRead(peerId: String)
}
