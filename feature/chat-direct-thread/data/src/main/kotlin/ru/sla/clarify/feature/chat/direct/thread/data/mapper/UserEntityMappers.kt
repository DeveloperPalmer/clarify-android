package ru.sla.clarify.feature.chat.direct.thread.data.mapper

import ru.sla.clarify.database.entity.UserEntity
import ru.sla.clarify.entity.chat.Peer

internal fun UserEntity.toPeer(): Peer {
  return Peer(
    id = Peer.Id(id.value),
    displayName = displayName,
    photoUrl = photoUrl
  )
}
