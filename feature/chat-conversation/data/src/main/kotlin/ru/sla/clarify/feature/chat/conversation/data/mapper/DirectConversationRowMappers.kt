package ru.sla.clarify.feature.chat.conversation.data.mapper

import ru.sla.clarify.database.entity.DirectConversationRow
import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.ConversationRecord
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.mapper.data.formatLastCommitTimestamp

internal fun DirectConversationRow.toDomainModel(): Conversation {
  return when (ConversationRecord.Type.fromValue(type)) {
    ConversationRecord.Type.Direct -> {
      Conversation.Direct(
        id = id,
        peer = Peer(
          id = Peer.Id(peerId.value),
          displayName = peerDisplayName,
          photoUrl = peerPhotoUrl
        ),
        lastCommit = lastCommit,
        lastCommitAt = formatLastCommitTimestamp(lastCommitTimestamp),
        lastCommitTimestamp = lastCommitTimestamp,
        unreadCount = unreadCount
      )
    }
    ConversationRecord.Type.Group -> {
      // TODO: @sla Conversation. Убрать маппер GroupConversationRow и собирать группу здесь, а не бросать ошибку
      error("unexpected conversation type: $type")
    }
  }
}
