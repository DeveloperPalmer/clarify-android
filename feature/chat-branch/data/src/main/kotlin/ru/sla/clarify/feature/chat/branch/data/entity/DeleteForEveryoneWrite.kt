package ru.sla.clarify.feature.chat.branch.data.entity

import ru.sla.clarify.entity.chat.LastCommitUpdate
import ru.sla.clarify.entity.chat.Peer

/** Денормализованные поля для удаления «у всех», посчитанные по полному кэшу до удаления. */
internal data class DeleteForEveryoneWrite(
  val peerId: Peer.Id,
  val lastCommit: LastCommitUpdate,
  val peerUnreadDelta: Int
)
