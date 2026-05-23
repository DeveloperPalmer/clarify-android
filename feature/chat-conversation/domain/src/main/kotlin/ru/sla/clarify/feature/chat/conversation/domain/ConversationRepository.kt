package ru.sla.clarify.feature.chat.conversation.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.chat.conversation.domain.entity.Participant

interface ConversationRepository {
  fun userId(): Flow<UserId>

  val conversations: Flow<List<Conversation>?>
  fun subscribeOnConversations(): Flow<Unit>
  suspend fun deleteConversations(ids: List<Conversation.Id>)

  fun subscribeOnUnreadCounts(): Flow<Unit>

  fun participant(conversationId: Conversation.Id, userId: UserId): Flow<Participant?>
}
