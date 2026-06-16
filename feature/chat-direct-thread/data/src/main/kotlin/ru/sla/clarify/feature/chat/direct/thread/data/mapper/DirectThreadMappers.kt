package ru.sla.clarify.feature.chat.direct.thread.data.mapper

import ru.sla.clarify.entity.chat.Peer

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
