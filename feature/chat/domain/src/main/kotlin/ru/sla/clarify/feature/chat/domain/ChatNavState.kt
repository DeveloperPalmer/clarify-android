package ru.sla.clarify.feature.chat.domain

import ru.sla.clarify.core.domain.di.scope.SingleIn
import ru.sla.clarify.feature.chat.domain.di.ChatScope
import javax.inject.Inject

/**
 * Holds the peer userId selected by the user when they navigate to a chat thread.
 *
 * This is a deliberately simple substitute for Way payload propagation and is scoped to the
 * chat flow lifecycle. [ChatFlowNode] writes to it before navigating to the thread screen,
 * and [ChatThreadModel] reads from it when it starts.
 */
@SingleIn(ChatScope::class)
class ChatNavState @Inject constructor() {
  @Volatile
  var peerUserId: String? = null

  fun requirePeerUserId(): String = requireNotNull(peerUserId) {
    "ChatNavState.peerUserId must be set before navigating to the thread screen"
  }
}
