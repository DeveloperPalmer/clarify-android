package ru.sla.clarify.feature.chat.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import ru.sla.clarify.core.domain.ReactiveModel
import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import ru.sla.clarify.feature.chat.domain.entity.ChatMessage
import javax.inject.Inject

@SingleIn(ChatScope::class)
class ChatThreadModel @Inject constructor(
  private val chatRepository: ChatRepository,
  private val chatNavState: ChatNavState
) : ReactiveModel() {

  /**
   * New incoming messages for the currently selected peer. Resolves the peer lazily so
   * that subscribers always observe the messages of whichever thread is currently active.
   */
  val incomingMessages: Flow<ChatMessage> = flow {
    emitAll(chatRepository.observeMessages(chatNavState.requirePeerUserId()))
  }

  val loadInitial = task<List<ChatMessage>>(name = "loadInitial") {
    chatRepository.loadHistory(
      peerUserId = chatNavState.requirePeerUserId(),
      before = null
    )
  }

  val sendText = task<String, ChatMessage>(name = "sendText") { text ->
    chatRepository.sendText(
      peerUserId = chatNavState.requirePeerUserId(),
      text = text
    )
  }

  val markRead = task<Unit>(name = "markRead") {
    chatRepository.markConversationRead(chatNavState.requirePeerUserId())
  }
}
