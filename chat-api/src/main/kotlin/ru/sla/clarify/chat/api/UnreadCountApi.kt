package ru.sla.clarify.chat.api

import kotlinx.coroutines.flow.Flow

/**
 * Счётчики непрочитанного беседы и ветки.
 *
 * Тема из `wiki/method/`: интерфейс отделён от остальных, потому что им не нужно знать
 * друг о друге, и это же деление даёт имя операции в спеке.
 */
interface UnreadCountApi {

  fun branchUnreadCountLive(
    conversationId: String,
    branchId: String
  ): Flow<Long>

  fun unreadCountLive(conversationId: String): Flow<Long>

  suspend fun updateBranchUnreadCount(conversationId: String, branchId: String)

  suspend fun updateUnreadCount(conversationId: String)
}
