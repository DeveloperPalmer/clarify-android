package ru.sla.clarify.feature.chat.direct.thread.data.mapper

import ru.sla.clarify.core.domain.entity.UserId
import ru.sla.clarify.entity.chat.Peer

@Suppress("UnusedParameter") // Unuser only for peer
internal fun mapToPeer(
  id: UserId,
  email: String,
  displayName: String,
  photoUrl: String?
): Peer {
  return Peer(
    id = Peer.Id(id.value),
    displayName = displayName,
    photoUrl = photoUrl
  )
}
