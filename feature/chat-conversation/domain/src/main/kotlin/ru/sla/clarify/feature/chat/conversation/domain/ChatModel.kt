package ru.sla.clarify.feature.chat.conversation.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.conversation.domain.di.ConversationScope
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import javax.inject.Inject

@SingleIn(ConversationScope::class)
class ChatModel @Inject constructor(
  private val conversationRepository: ConversationRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    conversationRepository.subscribeOnConversations()
      .launchIn(scope)
  }

  val fetchConversations = task<Unit>(name = "fetchConversations") {
    conversationRepository.conversations.first()
  }

  val deleteConversations = task<List<Conversation.Id>, Unit>(name = "deleteConversation") { ids ->
    conversationRepository.deleteConversations(ids = ids)
  }

  val userId: Flow<String?> = conversationRepository
    .userId()
    .map { it?.value }

  val conversations: Flow<List<Conversation>> = conversationRepository.conversations
    .filterNotNull()
}
