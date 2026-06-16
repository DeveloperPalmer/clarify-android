package ru.sla.clarify.feature.chat.direct.thread.data.mapper

import ru.sla.clarify.entity.chat.Commit
import ru.sla.clarify.entity.chat.Peer
import java.time.LocalDateTime

@Suppress("UnusedParameter") // Unuser only for peer
internal fun mapToPeer(
  id: String,
  email: String,
  displayName: String,
  photoUrl: String?
): Peer {
  return Peer(
    id = Peer.Id(id),
    displayName = displayName,
    photoUrl = photoUrl
  )
}

internal fun Commit.withReadStatus(peerLastReadAt: LocalDateTime?): Commit {
  if (this !is Commit.Message || !isSelf) return this
  if (status != Commit.Status.Sent || peerLastReadAt == null) return this
  return if (timestamp.isAfter(peerLastReadAt)) this else copy(status = Commit.Status.Read)
}
