package ru.sla.clarify.feature.chat.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import javax.inject.Inject

@SingleIn(ChatScope::class)
class ChatThreadModel @Inject constructor(
  private val chatRepository: ChatRepository
) : ReactiveModel() {

  private val stateFlow = MutableStateFlow(State())

  fun savePeerId(peerId: String) {
    stateFlow.update { it.copy(peerId = peerId) }
  }

  fun readPeerId(): String {
    return requireNotNull(stateFlow.value.peerId) {
      "ChatThreadModel: peerId not found in cache"
    }
  }

  val loadHistory = task<List<ChatMessage>>(name = "loadInitial") {
    chatRepository.loadHistory(
      count = DEFAULT_HISTORY_PAGE_SIZE,
      peerId = readPeerId()
    )
  }

  val sendText = task<String, ChatMessage>(name = "sendText") { text ->
    chatRepository.sendText(
      peerId = readPeerId(),
      text = text
    )
  }

  val markRead = task<Unit>(name = "markRead") {
    chatRepository.markConversationRead(readPeerId())
  }

  val treadMessages: Flow<ChatMessage>
    get() = chatRepository.treadMessages(readPeerId())

  private data class State(
    val peerId: String? = null
  )
}

private const val DEFAULT_HISTORY_PAGE_SIZE: Int = 20
