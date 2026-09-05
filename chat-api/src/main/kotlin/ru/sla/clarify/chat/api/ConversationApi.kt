package ru.sla.clarify.chat.api

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.GroupName
import ru.sla.clarify.entity.chat.ChatChange
import ru.sla.clarify.entity.chat.ConversationRecord

/**
 * Беседы: живой список, создание группы, переименование и удаление.
 *
 * Тема из `wiki/method/`: интерфейс отделён от остальных, потому что им не нужно знать
 * друг о друге, и это же деление даёт имя операции в спеке.
 */
interface ConversationApi {

  suspend fun deleteConversations(ids: List<String>)

  suspend fun deleteConversation(conversationId: String)

  fun conversationsLive(): Flow<List<ChatChange<ConversationRecord>>>

  suspend fun createGroupConversation(name: GroupName): String

  suspend fun updateConversationName(conversationId: String, name: String)
}
