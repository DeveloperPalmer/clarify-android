package ru.sla.clarify.feature.chat.direct.thread.domain.entity

import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer

sealed interface ThreadTarget {
  data class Direct(val peerId: Peer.Id) : ThreadTarget
  data class Group(val conversationId: Conversation.Id) : ThreadTarget
}
