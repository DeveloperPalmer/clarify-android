package ru.sla.clarify.feature.chat.conversation.data.mapper

import app.cash.sqldelight.Query
import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.database.chat.ChatConversationQueries
import ru.sla.clarify.feature.chat.conversation.domain.entity.Conversation
import ru.sla.clarify.feature.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.FirestoreSchema

internal fun ChatConversationQueries.selectAll(userId: UserId): Query<Conversation> {
  return selectAllWithPeer(currentUserId = userId.value) {
      id,
      type,
      lastCommit,
      lastCommitTimestamp,
      unreadCount,
      peerId,
      peerDisplayName,
      peerPhotoUrl
    ->

    when (type) {
      FirestoreSchema.ConversationType.Direct.value -> {
        Conversation.Direct(
          id = Conversation.Id(id),
          peer = Peer(
            id = Peer.Id(requireNotNull(peerId)),
            displayName = peerDisplayName,
            photoUrl = peerPhotoUrl
          ),
          lastMessage = lastCommit,
          lastMessageTimestamp = lastCommitTimestamp,
          unreadCount = unreadCount
        )
      }
      else -> error("unexpected conversation type: $type")
    }
  }
}
