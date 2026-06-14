package ru.sla.clarify.feature.chat.conversation.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.GroupName
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.feature.entity.chat.Conversation
import ru.sla.clarify.feature.entity.chat.Peer

interface ConversationRepository {
  suspend fun subscribeOnConversations()
  suspend fun subscribeOnMemberProfiles()
  suspend fun subscribeOnConversationsUnreadCounts()

  suspend fun deleteConversations(ids: List<Conversation.Id>)

  suspend fun fetchCurrentUser()
  suspend fun getPeerByEmail(email: Email): Peer.Id
  suspend fun createGroup(name: GroupName): Conversation.Id

  val user: Flow<User?>
  val conversations: Flow<List<Conversation>?>
}
