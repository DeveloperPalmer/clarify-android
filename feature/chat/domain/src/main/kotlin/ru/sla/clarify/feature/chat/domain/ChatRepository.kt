package ru.sla.clarify.feature.chat.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.feature.chat.domain.entity.Conversation

interface ChatRepository {
  /**
   * Stream of all C2C conversations the current user has. Emits a fresh snapshot whenever the
   * underlying SDK reports new/changed/deleted conversations.
   */
  fun observeConversations(): Flow<List<Conversation>>

  /**
   * Forces a one-time refresh of the conversation list from the server.
   */
  suspend fun loadConversations(): List<Conversation>

  /**
   * Stream of new incoming messages for the given peer. Only messages sent by [peerUserId]
   * are emitted; messages sent by the current user are not included here (they are returned
   * synchronously from [sendText]).
   */
  fun observeMessages(peerUserId: String): Flow<ChatMessage>

  /**
   * Loads up to [count] historical messages older than [before] (or the most recent ones
   * if [before] is null). The result is ordered from newest to oldest.
   */
  suspend fun loadHistory(
    peerUserId: String,
    before: ChatMessage?,
    count: Int = DEFAULT_HISTORY_PAGE_SIZE
  ): List<ChatMessage>

  suspend fun sendText(peerUserId: String, text: String): ChatMessage

  suspend fun markConversationRead(peerUserId: String)

  /**
   * Returns the userId under which the current device is logged in to the chat SDK,
   * or null if no user is currently logged in.
   */
  fun getCurrentUserId(): String?

  companion object {
    const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
  }
}
