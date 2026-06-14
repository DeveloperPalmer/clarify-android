package ru.sla.clarify.feature.chat.conversation.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer

interface ConversationRepository {
  suspend fun subscribeOnConversations()
  suspend fun subscribeOnParticipantProfiles()
  suspend fun subscribeOnConversationsUnreadCounts()

  suspend fun deleteConversations(ids: List<Conversation.Id>)

  suspend fun fetchCurrentUser()
  suspend fun getPeerByEmail(email: Email): Peer.Id
  suspend fun createGroup(name: String): Conversation.Id

  val user: Flow<User?>
  val conversations: Flow<List<Conversation>?>
}
