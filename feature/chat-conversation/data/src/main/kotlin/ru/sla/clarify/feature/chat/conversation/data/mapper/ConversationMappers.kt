package ru.sla.clarify.feature.chat.conversation.data.mapper

import ru.sla.clarify.entity.chat.Conversation
import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.entity.ConversationNM
import ru.sla.clarify.mapper.formatLastCommitTimestamp

internal fun mapToConversation(
  id: String,
  type: String,
  lastCommit: String?,
  lastCommitTimestamp: Long,
  unreadCount: Long,
  peerId: String,
  peerDisplayName: String,
  peerPhotoUrl: String?
): Conversation {
  return when (ConversationNM.Type.entries.first { it.value == type }) {
    ConversationNM.Type.Direct -> {
      Conversation.Direct(
        id = Conversation.Id(id),
        peer = Peer(
          id = Peer.Id(peerId),
          displayName = peerDisplayName,
          photoUrl = peerPhotoUrl
        ),
        lastCommit = lastCommit,
        lastCommitAt = formatLastCommitTimestamp(lastCommitTimestamp),
        lastCommitTimestamp = lastCommitTimestamp,
        unreadCount = unreadCount
      )
    }
    ConversationNM.Type.Group -> {
      // TODO: @sla Conversation. Remove mapToGroup. Add Group mapper here instead of throw error
      error("unexpected conversation type: $type")
    }
  }
}

// TODO: @sla Conversation. Remove "Suppress" when remove UnusedParameter
@Suppress("UnusedParameter")
internal fun mapToGroup(
  id: String,
  name: String?,
  // TODO: @sla Conversation. Remove unused "ownerUid"
  ownerUid: String?,
  lastCommit: String?,
  // TODO: @sla Conversation. Remove unused "fake"
  fake: String?,
  lastCommitTimestamp: Long,
  unreadCount: Long,
  // TODO: @sla Conversation. Remove unused "memberCount"
  memberCount: Long,
  lastCommitSenderDisplayName: String?
): Conversation.Group {
  return Conversation.Group(
    id = Conversation.Id(id),
    name = name.orEmpty(),
    lastCommit = lastCommit,
    lastCommitSenderName = lastCommitSenderDisplayName,
    lastCommitAt = formatLastCommitTimestamp(lastCommitTimestamp),
    lastCommitTimestamp = lastCommitTimestamp,
    unreadCount = unreadCount
  )
}
