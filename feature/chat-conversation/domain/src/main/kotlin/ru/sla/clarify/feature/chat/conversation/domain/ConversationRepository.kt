package ru.sla.clarify.feature.chat.conversation.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation

interface ConversationRepository {
  fun userId(): Flow<UserId?>

  fun subscribeOnConversations(): Flow<Unit>
  suspend fun deleteConversations(ids: List<Conversation.Id>)

  val conversations: Flow<List<Conversation>?>
}
