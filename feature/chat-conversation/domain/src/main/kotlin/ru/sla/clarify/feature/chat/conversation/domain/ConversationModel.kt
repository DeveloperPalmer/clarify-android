package ru.sla.clarify.feature.chat.conversation.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.Email
import ru.sla.clarify.core.domain.entity.GroupName
import ru.sla.clarify.core.domain.entity.User
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import javax.inject.Inject

@SingleIn(ConversationScope::class)
class ConversationModel @Inject constructor(
  private val conversationRepository: ConversationRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    scope.launch { conversationRepository.fetchCurrentUser() }
    scope.launch { conversationRepository.subscribeOnConversations() }
    scope.launch { conversationRepository.subscribeOnConversationsUnreadCounts() }
    scope.launch { conversationRepository.subscribeOnMemberProfiles() }
  }

  val getPeerByEmail = task<Email, Peer.Id>(
    name = "getPeerByEmail"
  ) { email ->
    conversationRepository.getPeerByEmail(email)
  }

  val deleteConversations = task<List<Conversation.Id>, Unit>(
    name = "deleteConversations"
  ) { ids ->
    conversationRepository.deleteConversations(ids)
  }

  val createGroup = task<GroupName, Conversation.Id>(
    name = "createGroup"
  ) { name ->
    conversationRepository.createGroupConversation(name)
  }

  val user: Flow<User?> = conversationRepository.user

  val conversations: Flow<List<Conversation>> = conversationRepository.conversations
    .filterNotNull()
}
