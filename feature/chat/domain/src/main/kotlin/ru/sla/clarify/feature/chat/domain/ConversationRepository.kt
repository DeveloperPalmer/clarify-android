package ru.sla.clarify.feature.chat.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.feature.chat.domain.entity.Conversation

interface ConversationRepository {
  fun getCurrentUserId(): String?

  fun subscribeOnConversations(): Flow<Unit>
  val conversations: Flow<List<Conversation>?>

  suspend fun deleteConversations(ids: List<Conversation.Id>)
}
