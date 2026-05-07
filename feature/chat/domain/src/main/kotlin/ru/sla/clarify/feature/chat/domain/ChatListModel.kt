package ru.sla.clarify.feature.chat.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import ru.sla.clarify.feature.chat.domain.entity.Conversation
import javax.inject.Inject

@SingleIn(ChatScope::class)
class ChatListModel @Inject constructor(
  private val chatRepository: ChatRepository
) : ReactiveModel() {

  override fun onPostStart() {
    super.onPostStart()
    chatRepository.subscribeOnConversations()
      .launchIn(scope)
  }

  val conversations: Flow<List<Conversation>> = chatRepository.conversations

  val currentUserId: String? get() = chatRepository.getCurrentUserId()

  val refresh = task<Unit>(name = "refresh") {
  }
}
