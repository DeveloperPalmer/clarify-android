package ru.sla.clarify.feature.chat.conversation.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.core.domain.entity.Email
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
    conversationRepository.subscribeOnConversations()
      .launchIn(scope)
    conversationRepository.subscribeOnUnreadCounts()
      .launchIn(scope)
  }

  val getPeerByEmail = task<Email, Peer.Id>(
    name = "getPeerByEmail"
  ) { email ->
    conversationRepository.getPeerByEmail(email)
  }

  val fetchConversations = task<Unit>(
    name = "fetchConversations"
  ) {
    conversationRepository.conversations.first()
  }

  val deleteConversations = task<List<Conversation.Id>, Unit>(
    name = "deleteConversations"
  ) { ids ->
    conversationRepository.deleteConversations(ids = ids)
  }

  val email: Flow<Email?> = conversationRepository.email()

  val conversations: Flow<List<Conversation>> = conversationRepository.conversations
    .filterNotNull()
}
