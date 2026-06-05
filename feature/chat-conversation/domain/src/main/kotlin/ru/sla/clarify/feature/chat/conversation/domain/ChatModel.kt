package ru.sla.clarify.feature.chat.conversation.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer
import javax.inject.Inject

@SingleIn(ConversationScope::class)
class ChatModel @Inject constructor(
  private val conversationRepository: ConversationRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    scope.launch { conversationRepository.subscribeOnConversations() }
    scope.launch { conversationRepository.subscribeOnParticipantProfiles() }
    scope.launch { conversationRepository.subscribeOnConversationsUnreadCounts() }
    scope.launch { conversationRepository.fetchCurrentUser() }
  }

  val getPeerByEmail = task<Email, Peer.Id>(
    name = "getPeerByEmail"
  ) { email ->
    conversationRepository.getPeerByEmail(email)
  }

  val deleteConversations = task<List<Conversation.Id>, Unit>(
    name = "deleteConversations"
  ) { ids ->
    conversationRepository.deleteConversations(ids = ids)
  }

  val user: Flow<User?> = conversationRepository.user

  val conversations: Flow<List<Conversation>> = conversationRepository.conversations.filterNotNull()
}
