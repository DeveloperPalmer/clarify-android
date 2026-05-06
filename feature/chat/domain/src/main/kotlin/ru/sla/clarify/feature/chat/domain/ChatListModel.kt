package ru.sla.clarify.feature.chat.domain

import kotlinx.coroutines.flow.Flow
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import ru.sla.clarify.feature.chat.domain.entity.Conversation
import javax.inject.Inject

@SingleIn(ChatScope::class)
class ChatListModel @Inject constructor(
  private val chatRepository: ChatRepository
) : ReactiveModel() {

  val conversations: Flow<List<Conversation>> = chatRepository.observeConversations()

  val currentUserId: String? get() = chatRepository.getCurrentUserId()

  val refresh = task<Unit>(name = "refresh") {
    chatRepository.loadConversations()
  }
}
