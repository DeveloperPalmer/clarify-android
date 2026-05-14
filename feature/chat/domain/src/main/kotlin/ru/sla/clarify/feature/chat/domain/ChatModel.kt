package ru.sla.clarify.feature.chat.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import ru.sla.clarify.feature.chat.domain.entity.Conversation
import javax.inject.Inject

@SingleIn(ChatScope::class)
class ChatModel @Inject constructor(
  private val conversationRepository: ConversationRepository,
  private val messageRepository: MessageRepository
) : ReactiveModel() {

  private val stateFlow = MutableStateFlow(State())

  override fun onPostStart() {
    super.onPostStart()
    conversationRepository.subscribeOnConversations()
      .launchIn(scope)
  }

  val fetchConversations = task<Unit>(name = "fetchConversations") {
    conversationRepository.conversations.first()
  }

  val loadHistory = task<List<ChatMessage>>(name = "loadHistory") {
    messageRepository.history(
      count = DEFAULT_HISTORY_PAGE_SIZE,
      peerId = requireNotNull(stateFlow.value.peerId)
    )
  }

  val sendText = task<String, ChatMessage>(name = "sendText") { text ->
    messageRepository.send(
      text = text,
      peerId = requireNotNull(stateFlow.value.peerId),
      parentId = null
    )
  }

  val deleteConversation = task<Conversation.Id, Unit>(name = "deleteConversation") { id ->
    conversationRepository.deleteConversation(id = id)
  }

  fun markReadTreadMessages() {
    scope.launch {
      messageRepository.markAsRead(
        peerId = requireNotNull(stateFlow.value.peerId)
      )
    }
  }

  fun setPeerId(id: String) {
    stateFlow.update { it.copy(peerId = id) }
  }

  fun requirePeerId(): String {
    return requireNotNull(stateFlow.value.peerId)
  }

  fun treadMessage(peerId: String): Flow<ChatMessage> {
    return messageRepository.peerMessages(peerId)
  }

  val currentUserId: String?
    get() = conversationRepository.getCurrentUserId()

  val conversations: Flow<List<Conversation>> = conversationRepository.conversations
    .filterNotNull()

  private data class State(
    val peerId: String? = null
  )
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
