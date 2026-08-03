package ru.sla.clarify.feature.chat.branch.data.entity

import ru.sla.clarify.entity.chat.Peer
import ru.sla.clarify.lib.google.firestore.entity.write.LastCommitParams

/** Денормализованные поля для удаления «у всех», посчитанные по полному кэшу до удаления. */
internal data class DeleteForEveryoneWrite(
  val peerId: Peer.Id,
  val lastCommit: LastCommitParams,
  val peerUnreadDelta: Int
)
